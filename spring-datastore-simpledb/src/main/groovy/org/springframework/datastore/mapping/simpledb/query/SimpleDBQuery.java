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
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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
        StringBuilder query = new StringBuilder("select * from `" + domain + "`");
        if ( !criteria.getCriteria().isEmpty() ) {
            query.append(" where "); //things list TestEntity.list() result in empty criteria collection, so we should not have a 'where' clause at all
        }

        String clause = "";
        if (criteria instanceof Conjunction) {
            clause = buildCompositeClause(criteria, "AND");
        } else if (criteria instanceof Disjunction) {
            clause = buildCompositeClause(criteria, "OR");
        } else {
            throw new RuntimeException("not implemented: " + criteria.getClass());
        }

        query.append(clause);
        List<Item> items = simpleDBTemplate.query(query.toString());
        List results = new LinkedList();
        for (Item item : items) {
            results.add(createObjectFromItem(item));
        }

        return results;
    }

    private String buildCompositeClause(Junction criteria, String booleanOperator) {
        StringBuilder clause = new StringBuilder();
        boolean first = true;
        for (Criterion criterion : criteria.getCriteria()) {
            if (criterion instanceof PropertyCriterion) {
                PropertyCriterion propertyCriterion = (PropertyCriterion) criterion;
                String propertyName = propertyCriterion.getProperty();
                PersistentProperty prop = simpleDBEntityPersister.getPersistentEntity().getPropertyByName(propertyName);
                if (prop == null) {
                    throw new IllegalArgumentException(
                            "Could not find property '" + propertyName + "' in entity '" + entity.getName() + "'");
                }

                KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
                String key = kv.getKey();

                if ( first ) {
                    //do nothing first time
                    first = false;
                } else {
                    clause.append(" ").append(booleanOperator).append(" "); //prepend with operator
                }

                if (Equals.class.equals(criterion.getClass())) {
                    clause.append(key + " = '" + propertyCriterion.getValue() + "'");
                } else {
                    throw new UnsupportedOperationException("Queries of type " + criterion.getClass()
                            .getSimpleName() + " are not supported by this implementation");
                }
            } else {
                throw new UnsupportedOperationException("Queries of type " + criterion.getClass()
                        .getSimpleName() + " are not supported by this implementation");

            }
        }
        return clause.toString();
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
