package org.springframework.datastore.mapping.simpledb.query;

import com.amazonaws.services.simpledb.model.Item;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.simpledb.engine.NativeSimpleDBItem;
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBDomainResolver;
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBEntityPersister;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBConverterUtil;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A {@link org.springframework.datastore.mapping.query.Query} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBQuery extends Query {
    public SimpleDBQuery(Session session, PersistentEntity entity, SimpleDBDomainResolver domainResolver, SimpleDBEntityPersister simpleDBEntityPersister, SimpleDBTemplate simpleDBTemplate) {
        super(session, entity);
        this.domainResolver = domainResolver;
        this.simpleDBEntityPersister = simpleDBEntityPersister;
        this.simpleDBTemplate = simpleDBTemplate;
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        String domain = domainResolver.getAllDomainsForEntity().get(
                0); //todo - in case of sharding we should iterate over all domains for this PersistentEntity (ideally in parallel)

        final List<Projection> projectionList = projections().getProjectionList();
        Projection countProjection = null;

        StringBuilder query;
        if ( projectionList.isEmpty() ) {
            query = new StringBuilder("select * from `" + domain + "`");
        } else {
            //AWS SimpleDB only supports count(*) projection, nothing else.
            countProjection = projectionList.get(0);
            if ( !CountProjection.class.equals(countProjection.getClass())  ) {
                throw new UnsupportedOperationException("Currently projections of type " + countProjection.getClass()
                        .getSimpleName() + " are not supported by this implementation");
            }

            query = new StringBuilder("select count(*) from `" + domain + "`");
        }

        if (!criteria.getCriteria().isEmpty()) {
            query.append(" where "); //things like TestEntity.list() result in empty criteria collection, so we should not have a 'where' clause at all
        }

        String clause = "";
        Set<String> usedPropertyNames = new HashSet<String>();
        if (criteria instanceof Conjunction) {
            clause = buildCompositeClause(criteria, "AND", usedPropertyNames);
        } else if (criteria instanceof Disjunction) {
            clause = buildCompositeClause(criteria, "OR", usedPropertyNames);
        } else {
            throw new RuntimeException("not implemented: " + criteria.getClass());
        }

        query.append(clause);

        List<Order> orderBys = getOrderBy();
        if ( !orderBys.isEmpty() ) {
            if ( orderBys.size() >1 ) {
                throw new UnsupportedOperationException("Only single 'order by' clause is supported. You have: "+orderBys.size());
            }
            Order orderBy = orderBys.get(0);
            String orderByPropertyName = orderBy.getProperty();
            String key = extractPropertyKey(orderByPropertyName);
            //AWS SimpleDB rule: if you use ORDER BY then you have to have a condition of that attribute in the where clause, otherwise it will throw an error
            //so we check if that property was used in the clause and if not we add 'is not null condition'
            if ( !usedPropertyNames.contains(orderByPropertyName) ) {
                query.append(" AND "+key+" IS NOT NULL");
            }

            query.append(" ORDER BY "+key+" "+orderBy.getDirection());
        }

        //specify the limit on the returned results
        int limit = max < 0 ? 2500 : max; //if user did not explicitly limit maxResults, use the maximum limit allowe dy AWS (if not specified explicitly it will use 100 limit)
        query.append(" LIMIT "+limit);

        List<Item> items = simpleDBTemplate.query(query.toString());
        List results = new LinkedList();
        if ( countProjection == null ) {
            for (Item item : items) {
                results.add(createObjectFromItem(item));
            }
        } else {
            int count = Integer.parseInt(items.get(0).getAttributes().get(0).getValue());
            results.add(count);
        }

        return results;
    }

    private String buildCompositeClause(Junction criteria, String booleanOperator, Set<String> usedPropertyNames) {
        StringBuilder clause = new StringBuilder();
        boolean first = true;
        for (Criterion criterion : criteria.getCriteria()) {
            if (first) {
                //do nothing first time
                first = false;
            } else {
                clause.append(" ").append(booleanOperator).append(" "); //prepend with operator
            }
            
            if (criterion instanceof PropertyCriterion) {
                PropertyCriterion propertyCriterion = (PropertyCriterion) criterion;
                String propertyName = propertyCriterion.getProperty();

                usedPropertyNames.add(propertyName); //register the fact that the property did have some condition - it is needed if we use order by clause
                if (Equals.class.equals(criterion.getClass())) {
                    String key = extractPropertyKey(propertyName);
                    Object value = propertyCriterion.getValue();
                    String stringValue =  SimpleDBConverterUtil.convertToString(value, entity.getMappingContext());
                    
                    clause.append(key + " = '" + stringValue + "'");
                } else if (IdEquals.class.equals(criterion.getClass())) {
                    clause.append("itemName() = '" + propertyCriterion.getValue() + "'");
                } else if (Like.class.equals(criterion.getClass())) {
                    String key = extractPropertyKey(propertyName);
                    clause.append(key + " LIKE '" + propertyCriterion.getValue() + "'");
                } else {
                    throw new UnsupportedOperationException("Queries of type " + criterion.getClass()
                            .getSimpleName() + " are not supported by this implementation");
                }
            } else if ( criterion instanceof Conjunction ) {
                String innerClause = buildCompositeClause((Conjunction) criterion, "AND", usedPropertyNames);
                addToMainClause(criteria, clause, innerClause);
            } else if ( criterion instanceof Disjunction ) {
                String innerClause = buildCompositeClause((Disjunction) criterion, "OR", usedPropertyNames);
                addToMainClause(criteria, clause, innerClause);
            } else {
                throw new UnsupportedOperationException("Queries of type " + criterion.getClass()
                        .getSimpleName() + " are not supported by this implementation");

            }
        }
        return clause.toString();
    }

    private void addToMainClause(Junction criteria, StringBuilder clause, String innerClause) {
        boolean useParenthesis = criteria.getCriteria().size() > 1; //use parenthesis only when needed
        if ( useParenthesis ) {
            clause.append("(");
        }
        clause.append(innerClause);
        if ( useParenthesis ) {
            clause.append(")");
        }
    }

    protected String extractPropertyKey(String propertyName) {
        PersistentProperty prop = simpleDBEntityPersister.getPersistentEntity().getPropertyByName(propertyName);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "Could not find property '" + propertyName + "' in entity '" + entity.getName() + "'");
        }

        KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
        String key = kv.getKey();
        return key;
    }

    protected Object createObjectFromItem(Item item) {
        final String id = item.getName();
        return simpleDBEntityPersister.createObjectFromNativeEntry(getEntity(), (Serializable) id,
                new NativeSimpleDBItem(item));
    }

    protected SimpleDBDomainResolver domainResolver;
    protected SimpleDBTemplate simpleDBTemplate;
    protected SimpleDBEntityPersister simpleDBEntityPersister;
}
