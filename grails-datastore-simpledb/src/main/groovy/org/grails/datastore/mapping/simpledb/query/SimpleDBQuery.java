/* Copyright (C) 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.simpledb.query;

import java.util.*;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.simpledb.engine.SimpleDBNativeItem;
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolver;
import org.grails.datastore.mapping.simpledb.engine.SimpleDBEntityPersister;
import org.grails.datastore.mapping.simpledb.util.SimpleDBConverterUtil;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate;

import com.amazonaws.services.simpledb.model.Item;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;

/**
 * A {@link org.grails.datastore.mapping.query.Query} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBQuery extends Query {

    protected SimpleDBDomainResolver domainResolver;
    protected SimpleDBTemplate simpleDBTemplate;
    protected SimpleDBEntityPersister simpleDBEntityPersister;

    protected static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    protected static final String ITEM_NAME = "itemName()";

    static{
        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            @Override
            public void handle(PersistentEntity entity, Equals criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, "=", stringValue);
            }
        });
        queryHandlers.put(NotEquals.class, new QueryHandler<NotEquals>() {
            @Override
            public void handle(PersistentEntity entity, NotEquals criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, "!=", stringValue);
            }
        });
        queryHandlers.put(IdEquals.class, new QueryHandler<IdEquals>() {
            @Override
            public void handle(PersistentEntity entity, IdEquals criterion, StringBuilder clause) {
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, ITEM_NAME, "=", stringValue);
            }
        });
        queryHandlers.put(Like.class, new QueryHandler<Like>() {
            @Override
            public void handle(PersistentEntity entity, Like criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, "LIKE", stringValue);
            }
        });
        queryHandlers.put(In.class, new QueryHandler<In>() {
            @Override
            public void handle(PersistentEntity entity, In criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);

                Collection<String> stringValues = SimpleDBConverterUtil.convertToStrings(criterion.getValues(), entity.getMappingContext());
                clause.append(key).append(" IN (");
                clause.append(SimpleDBUtil.quoteValues(stringValues)).append(")");
            }
        });
        queryHandlers.put(Between.class, new QueryHandler<Between>() {
            @Override
            public void handle(PersistentEntity entity, Between criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String fromStringValue = SimpleDBConverterUtil.convertToString(criterion.getFrom(), entity.getMappingContext());
                String toStringValue = SimpleDBConverterUtil.convertToString(criterion.getTo(), entity.getMappingContext());

                clause.append(key).append(" >= ").append(SimpleDBUtil.quoteValue(fromStringValue)).append(" AND ");
                clause.append(key).append(" <= ").append(SimpleDBUtil.quoteValue(toStringValue));
            }
        });
        queryHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            @Override
            public void handle(PersistentEntity entity, GreaterThan criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, ">", stringValue);
            }
        });
        queryHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            @Override
            public void handle(PersistentEntity entity, GreaterThanEquals criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, ">=", stringValue);
            }
        });
        queryHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            @Override
            public void handle(PersistentEntity entity, LessThan criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, "<", stringValue);
            }
        });
        queryHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            @Override
            public void handle(PersistentEntity entity, LessThanEquals criterion, StringBuilder clause) {
                String propertyName = criterion.getProperty();
                String key = getSmartQuotedKey(entity, propertyName);
                String stringValue = SimpleDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                addSimpleComparison(clause, key, "<=", stringValue);
            }
        });
    }

    public SimpleDBQuery(Session session, PersistentEntity entity, SimpleDBDomainResolver domainResolver,
               SimpleDBEntityPersister simpleDBEntityPersister, SimpleDBTemplate simpleDBTemplate) {
        super(session, entity);
        this.domainResolver = domainResolver;
        this.simpleDBEntityPersister = simpleDBEntityPersister;
        this.simpleDBTemplate = simpleDBTemplate;
    }

    @Override
    protected List executeQuery(@SuppressWarnings("hiding") PersistentEntity entity, @SuppressWarnings("hiding") Junction criteria) {
        // TODO - in case of sharding we should iterate over all domains for this PersistentEntity (ideally in parallel)
        String domain = domainResolver.getAllDomainsForEntity().get(0);

        final List<Projection> projectionList = projections().getProjectionList();
        Projection countProjection = null;

        StringBuilder query;
        if (projectionList.isEmpty()) {
            query = new StringBuilder("select * from `").append(domain).append("`");
        } else {
            //AWS SimpleDB only supports count(*) projection, nothing else.
            countProjection = projectionList.get(0);
            if (!CountProjection.class.equals(countProjection.getClass())) {
                throw new UnsupportedOperationException("Currently projections of type " +
                  countProjection.getClass().getSimpleName() + " are not supported by this implementation");
            }

            query = new StringBuilder("select count(*) from `").append(domain).append("`");
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
            throw new RuntimeException("not implemented: " + criteria.getClass().getName());
        }

        query.append(clause);

        List<Order> orderBys = getOrderBy();
        if (!orderBys.isEmpty()) {
            if (orderBys.size() > 1) {
                throw new UnsupportedOperationException("Only single 'order by' clause is supported. You have: " + orderBys.size());
            }
            @SuppressWarnings("hiding") Order orderBy = orderBys.get(0);
            String orderByPropertyName = orderBy.getProperty();
            String key = extractPropertyKey(orderByPropertyName, entity);
            //AWS SimpleDB rule: if you use ORDER BY then you have to have a condition on that attribute in the where clause, otherwise it will throw an error
            //so we check if that property was used in the clause and if not we add 'is not null' condition
            if (!usedPropertyNames.contains(orderByPropertyName)) {
                if (criteria.getCriteria().isEmpty()) { //we might have a case 'select * from X order by ABC' which must be fixed into 'select * from X where ABC IS NOT NULL order by ABC'
                    query.append(" where ");
                } else {
                    query.append(" AND ");
                }

                query.append(SimpleDBUtil.quoteName(key)).append(" IS NOT NULL");
            }

            query.append(" ORDER BY ").append(SimpleDBUtil.quoteName(key)).append(" ").append(orderBy.getDirection());
        }

        //specify the limit on the returned results
        int limit = max < 0 ? 2500 : max; //if user did not explicitly limit maxResults, use the maximum limit allowed dy AWS (if not specified explicitly it will use 100 limit)
        query.append(" LIMIT ").append(limit);

        List<Item> items = simpleDBTemplate.query(query.toString());
        List<Object> results = new LinkedList<Object>();
        if (countProjection == null) {
            for (Item item : items) {
                results.add(createObjectFromItem(item));
            }
        } else {
            int count = Integer.parseInt(items.get(0).getAttributes().get(0).getValue());
            results.add(count);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private String buildCompositeClause(@SuppressWarnings("hiding") Junction criteria,
            String booleanOperator, Set<String> usedPropertyNames) {
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

                QueryHandler queryHandler = queryHandlers.get(criterion.getClass());
                if (queryHandler != null) {
                    queryHandler.handle(entity, criterion, clause);
                } else {
                    throw new UnsupportedOperationException("Queries of type " +
                     criterion.getClass().getSimpleName() + " are not supported by this implementation");
                }
            } else if (criterion instanceof Conjunction) {
                String innerClause = buildCompositeClause((Conjunction) criterion, "AND", usedPropertyNames);
                addToMainClause(criteria, clause, innerClause);
            } else if (criterion instanceof Disjunction) {
                String innerClause = buildCompositeClause((Disjunction) criterion, "OR", usedPropertyNames);
                addToMainClause(criteria, clause, innerClause);
            } else if (criterion instanceof Negation) {
                String innerClause = buildCompositeClause((Negation) criterion, "OR", usedPropertyNames); //when we negate we use OR by default
                clause.append("NOT (").append(innerClause).append(")");
            } else {
                throw new UnsupportedOperationException("Queries of type " +
                     criterion.getClass().getSimpleName() + " are not supported by this implementation");
            }
        }
        return clause.toString();
    }

    private void addToMainClause(@SuppressWarnings("hiding") Junction criteria, StringBuilder clause, String innerClause) {
        boolean useParenthesis = criteria.getCriteria().size() > 1; //use parenthesis only when needed
        if (useParenthesis) {
            clause.append("(");
        }
        clause.append(innerClause);
        if (useParenthesis) {
            clause.append(")");
        }
    }

    protected Object createObjectFromItem(Item item) {
        final String id = item.getName();
        return simpleDBEntityPersister.createObjectFromNativeEntry(getEntity(), id,
                new SimpleDBNativeItem(item));
    }

    protected static interface QueryHandler<T> {
        public void handle(PersistentEntity entity, T criterion, StringBuilder clause);
    }

    protected static String extractPropertyKey(String propertyName, PersistentEntity entity) {
        PersistentProperty prop = entity.getPropertyByName(propertyName);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "Could not find property '" + propertyName + "' in entity '" + entity.getName() + "'");
        }

        KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
        String key = kv.getKey();
        return key;
    }

    /**
     * Assumes that the key is already quoted or is itemName()
     * @param clause
     * @param key
     * @param comparison
     * @param stringValue
     */
    protected static void addSimpleComparison(StringBuilder clause, String key, String comparison, String stringValue) {
        clause.append(key).append(" ").append(comparison).append(" ").append(SimpleDBUtil.quoteValue(stringValue));
    }

    /**
     * Returns quoted mapped key OR if the property is an identity returns 'itemName()' - this is how AWS SimpleDB refers to primary key field.
     * @param entity
     * @param propertyName
     * @return
     */
    protected static String getSmartQuotedKey(PersistentEntity entity, String propertyName) {
        if (entity.isIdentityName(propertyName)) {
            return ITEM_NAME;
        } else {
            return SimpleDBUtil.quoteName(extractPropertyKey(propertyName, entity));
        }
    }

}
