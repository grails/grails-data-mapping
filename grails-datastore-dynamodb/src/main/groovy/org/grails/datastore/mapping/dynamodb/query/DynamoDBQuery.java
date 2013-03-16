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
package org.grails.datastore.mapping.dynamodb.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBEntityPersister;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBNativeItem;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolver;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConverterUtil;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.order.ManualEntityOrdering;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;

/**
 * A {@link org.grails.datastore.mapping.query.Query} implementation for the DynamoDB store
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DynamoDBQuery extends Query {

    protected DynamoDBTableResolver tableResolver;
    protected DynamoDBTemplate dynamoDBTemplate;
    protected DynamoDBEntityPersister dynamoDBEntityPersister;

    protected static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();

    static {
        //see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_Scan.html
        //for a list of all dynamodb operators and their arguments

        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            public void handle(PersistentEntity entity, Equals criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                if (criterion.getValue() == null) {
                    //for null we have to use special operator
                    DynamoDBUtil.checkFilterForExistingKey(filter, key);
                    filter.put(key, new Condition().withComparisonOperator(ComparisonOperator.NULL.toString()));
                } else {
                    String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                    boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                    DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.EQ.toString(), stringValue, isNumber);
                }
            }
        });
        queryHandlers.put(NotEquals.class, new QueryHandler<NotEquals>() {
            public void handle(PersistentEntity entity, NotEquals criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                if (criterion.getValue() == null) {
                    //for null we have to use special operator
                    DynamoDBUtil.checkFilterForExistingKey(filter, key);
                    filter.put(key, new Condition().withComparisonOperator(ComparisonOperator.NOT_NULL.toString()));
                } else {
                    String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                    boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                    DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.NE.toString(), stringValue, isNumber);
                }
            }
        });
        queryHandlers.put(IdEquals.class, new QueryHandler<IdEquals>() {
            public void handle(PersistentEntity entity, IdEquals criterion, Map<String, Condition> filter) {
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());

                DynamoDBUtil.addSimpleComparison(filter, "id", ComparisonOperator.EQ.toString(), stringValue, false);
            }
        });
        queryHandlers.put(Like.class, new QueryHandler<Like>() {
            public void handle(PersistentEntity entity, Like criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                //dynamo db has only 'contains' and 'begins_with' operators, so we have to take out '%' and figure out which one to use

                String searchToken = stringValue;

                //begins_with is without % at all or (xxx% and not %xxx%)
                if (!searchToken.contains("%") || (searchToken.endsWith("%") && !searchToken.startsWith("%"))) {
                    if (searchToken.endsWith("%")) {
                        //kill % at the end
                        searchToken = searchToken.substring(0, searchToken.length() - 1);
                    }

                    //make sure % is not in the middle - we can't handle it with dynamo
                    if (searchToken.contains("%")) {
                        throw new IllegalArgumentException("DynamoDB can not handle % in the middle of search string. You specified: " + stringValue);
                    }

                    DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.BEGINS_WITH.toString(), searchToken, isNumber);
                } else {
                    //if we got here it has to start with %
                    //the only supported cases are %xxx and %xxx%

                    if (searchToken.endsWith("%")) {
                        //kill % at the end
                        searchToken = searchToken.substring(0, searchToken.length() - 1);
                    }

                    if (searchToken.startsWith("%")) {
                        //kill % at the beginning
                        searchToken = searchToken.substring(1, searchToken.length());
                    }

                    //make sure % is not in the middle - we can't handle it with dynamo
                    if (searchToken.contains("%")) {
                        throw new IllegalArgumentException("DynamoDB can not handle % in the middle of search string. You specified: " + stringValue);
                    }

                    DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.CONTAINS.toString(), searchToken, isNumber);
                }
            }
        });
        queryHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(PersistentEntity entity, In criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);

                DynamoDBUtil.checkFilterForExistingKey(filter, key);

                Collection<String> stringValues = DynamoDBConverterUtil.convertToStrings(criterion.getValues(), entity.getMappingContext());
                boolean isNumber = false;
                if (!criterion.getValues().isEmpty()) {
                    //all values should be of the same type, so take a look at the first
                    isNumber = DynamoDBConverterUtil.isNumber(criterion.getValues().iterator().next());
                }

                Collection<AttributeValue> attributeValues = new ArrayList<AttributeValue>();
                for (String stringValue : stringValues) {
                    DynamoDBUtil.addAttributeValue(attributeValues, stringValue, isNumber);
                }

                filter.put(key, new Condition().withComparisonOperator(ComparisonOperator.IN.toString()).
                        withAttributeValueList(attributeValues));
            }
        });
        queryHandlers.put(Between.class, new QueryHandler<Between>() {
            public void handle(PersistentEntity entity, Between criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String fromStringValue = DynamoDBConverterUtil.convertToString(criterion.getFrom(), entity.getMappingContext());
                String toStringValue = DynamoDBConverterUtil.convertToString(criterion.getTo(), entity.getMappingContext());

                DynamoDBUtil.checkFilterForExistingKey(filter, key);

                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getFrom());
                Collection<AttributeValue> attributeValues = new ArrayList<AttributeValue>();
                DynamoDBUtil.addAttributeValue(attributeValues, fromStringValue, isNumber);
                DynamoDBUtil.addAttributeValue(attributeValues, toStringValue, isNumber);

                filter.put(key, new Condition().withComparisonOperator(ComparisonOperator.BETWEEN.toString()).
                        withAttributeValueList(attributeValues));
            }
        });
        queryHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            public void handle(PersistentEntity entity, GreaterThan criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.GT.toString(), stringValue, isNumber);
            }
        });
        queryHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            public void handle(PersistentEntity entity, GreaterThanEquals criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.GE.toString(), stringValue, isNumber);
            }
        });
        queryHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            public void handle(PersistentEntity entity, LessThan criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.LT.toString(), stringValue, isNumber);
            }
        });
        queryHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            public void handle(PersistentEntity entity, LessThanEquals criterion, Map<String, Condition> filter) {
                String propertyName = criterion.getProperty();
                String key = getKey(entity, propertyName);
                String stringValue = DynamoDBConverterUtil.convertToString(criterion.getValue(), entity.getMappingContext());
                boolean isNumber = DynamoDBConverterUtil.isNumber(criterion.getValue());

                DynamoDBUtil.addSimpleComparison(filter, key, ComparisonOperator.LE.toString(), stringValue, isNumber);
            }
        });
    }

    public DynamoDBQuery(Session session, PersistentEntity entity, DynamoDBTableResolver tableResolver,
                         DynamoDBEntityPersister dynamoDBEntityPersister, DynamoDBTemplate dynamoDBTemplate) {
        super(session, entity);
        this.tableResolver = tableResolver;
        this.dynamoDBEntityPersister = dynamoDBEntityPersister;
        this.dynamoDBTemplate = dynamoDBTemplate;
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        String table = tableResolver.getAllTablesForEntity().get(0);

        final List<Projection> projectionList = projections().getProjectionList();
        boolean hasCountProjection = false;

        if (!projectionList.isEmpty()) {
            hasCountProjection = validateProjectionsAndCheckIfCountIsPresent(projectionList);
        }

        List<List<PropertyCriterion>> independentConditions = flattenAndReplaceDisjunction(criteria);
        List<Map<String, Condition>> filters = buildFilters(independentConditions);

        int maxToGet = max < 0 ? Integer.MAX_VALUE : max;
        boolean hasOrdering = !getOrderBy().isEmpty();

        List<Object> results;
        if (projectionList.isEmpty()) {
            if (!hasOrdering) {
                results = doGetItems(table, filters, maxToGet).objects;
            } else {
                //since we sort in memory, if we have ordering we should get all matches, then sort, then cut to the maximum size
                results = doGetItems(table, filters, Integer.MAX_VALUE).objects;
                results = handleOrdering(entity, results);
                results = resizeUpTo(results, maxToGet);
            }
        } else {
            if (hasCountProjection) { //count is returned by AWS in a special way...
                results = new ArrayList<Object>();
                if (independentConditions.size() == 1) {
                    //optimization - if we only have one query to run we can use scan with the count request
                    int count = dynamoDBTemplate.scanCount(table, filters.get(0));
                    results.add(count);
                } else {
                    //we have to actually get the items and return size of the collection because for queries '(a or b) and (a or c)' we can't return sum of counts
                    List<Object> loaded = doGetItems(table, filters, maxToGet).objects;
                    results.add(loaded.size());
                }
            } else {
                List<Order> orderBys = getOrderBy();

                if (!orderBys.isEmpty()) {
                    //too messy to implement for initial cut
                    throw new UnsupportedOperationException("'order by' can't be used with projections (not implemented yet). You have: " + orderBys.size());
                }

                List<Map<String, AttributeValue>> items = doGetItems(table, filters, maxToGet).items;
                results = new ArrayList<Object>();
                for (Projection projection : projectionList) {
                    if (IdProjection.class.equals(projection.getClass())) {
                        for (Map<String, AttributeValue> item : items) {
                            results.add(DynamoDBUtil.getAttributeValue(item, "id"));
                        }
                    } else if (PropertyProjection.class.equals(projection.getClass())) {
                        for (Map<String, AttributeValue> item : items) {
                            String key = extractPropertyKey(((PropertyProjection) projection).getPropertyName(), entity);
                            results.add(DynamoDBUtil.getAttributeValue(item, key));
                        }
                    }
                }
            }
        }

        return results;
    }

    private List<Object> resizeUpTo(List<Object> results, int max) {
        if (results.size() > max) {
            return results.subList(0, max);
        } else {
            return results;
        }
    }

    private List<Object> handleOrdering(PersistentEntity entity, List<Object> results) {
        ManualEntityOrdering ordering = new ManualEntityOrdering(entity);
        List<Order> orderBys = getOrderBy();
        results = ordering.applyOrder(results, orderBys);
        return results;
    }

    private Results doGetItems(String table, List<Map<String, Condition>> filters, int maxToGet) {
        List<Object> objects = new ArrayList<Object>();
        List<Map<String, AttributeValue>> resultItems = new ArrayList<Map<String, AttributeValue>>();
        Set alreadyLoadedIds = new HashSet();
        for (Map<String, Condition> filter : filters) {
            List<Map<String, AttributeValue>> items = dynamoDBTemplate.scan(table, filter, maxToGet);
            for (Map<String, AttributeValue> item : items) {
                Object id = DynamoDBUtil.getIdKey(item);
                if (!alreadyLoadedIds.contains(id)) {
                    if (objects.size() < maxToGet) {
                        objects.add(createObjectFromItem(item));
                        resultItems.add(item);
                        alreadyLoadedIds.add(id);
                    }
                }
            }
        }
        return new Results(objects, resultItems);
    }

    private List<Map<String, Condition>> buildFilters(List<List<PropertyCriterion>> independentConditions) {
        List<Map<String, Condition>> result = new ArrayList<Map<String, Condition>>();
        if (independentConditions.isEmpty()) {
            //if there are no criteria queries, create single dummy empty filter, otherwise we will not call dynamo at all
            if (independentConditions.isEmpty()) {
                result.add((Map<String, Condition>) Collections.EMPTY_MAP);
            }
        } else {
            for (List<PropertyCriterion> query : independentConditions) {
                Map<String, Condition> filter = new HashMap<String, Condition>();
                for (PropertyCriterion propertyCriterion : query) {
                    QueryHandler queryHandler = queryHandlers.get(propertyCriterion.getClass());
                    if (queryHandler != null) {
                        queryHandler.handle(entity, propertyCriterion, filter);
                    } else {
                        throw new UnsupportedOperationException("Queries of type " +
                                propertyCriterion.getClass().getSimpleName() + " are not supported by this implementation");
                    }
                }
                result.add(filter);
            }
        }

        return result;
    }

    /**
     * make sure that only property, id, or count projections are provided, and that the combination of them is meaningful.
     * Throws exception if something is invalid.
     *
     * @param projections
     * @returns true if count projection is present, false otherwise.
     */
    private boolean validateProjectionsAndCheckIfCountIsPresent(List<Projection> projections) {
        //of the grouping projects AWS DynamoDB only supports count(*) projection, nothing else. Other kinds will have
        //to be explicitly coded later...
        boolean hasCountProjection = false;
        for (Projection projection : projections) {
            if (!(PropertyProjection.class.equals(projection.getClass()) ||
                    IdProjection.class.equals(projection.getClass()) ||
                    CountProjection.class.equals(projection.getClass())
            )) {
                throw new UnsupportedOperationException("Currently projections of type " +
                        projection.getClass().getSimpleName() + " are not supported by this implementation");
            }

            if (CountProjection.class.equals(projection.getClass())) {
                hasCountProjection = true;
            }
        }
        if (projections.size() > 1 && hasCountProjection) {
            throw new IllegalArgumentException("Can not mix count projection and other types of projections. You requested: " + projections);
        }
        return hasCountProjection;
    }

    /**
     * Recurses into the specified junction and flattens it into a list. Each top-level element in this list
     * represents queries on properties (meant as conjunction queries) which must be fired independently of each other, and then later
     * combined to get unique set of matching elements. This is needed because dynamodb does not support OR queries in any shape of form.
     * For example, to illustrate behavior of this method:
     * just property a ==> [ [a] ]   //1 query
     * a and b = con(a,b) ==> [ [a,b] ]   //1 query
     * a or b = dis(a,b) ==>  [ [a], [b] ] //2 queries: 1 for a, 1 for b
     * (a or b) and c = Con( Dis(a,b) , c  ) = [Con(a,c), con(b,c)] ==> [ [a,c], [b,c] ] // 2 queries - 1 for a&c, 1 for b&c
     * (a or b) and c,d = Con( Dis(a,b) , c , d) = [Con(a,c,d), con(b,c,d)] ==> [ [a,c,d], [b,c,d] ] //2 queries
     * (a and b) and c = con(con(a,b), c) ==> [ [a,b,c] ] //1 query
     * (a and b) or c = dis(con(a,b), c) ==> [ [a,b], [c] ] //2 queries
     *
     * @param criteria
     * @return
     */
    private List<List<PropertyCriterion>> flattenAndReplaceDisjunction(Junction criteria) {
        List<List<PropertyCriterion>> result = new ArrayList<List<PropertyCriterion>>();

        if (criteria instanceof Conjunction) {
            List<List<PropertyCriterion>> temp = handleConjunction((Conjunction) criteria);
            result.addAll(temp);
        } else if (criteria instanceof Disjunction) {
            handleDisjunction(criteria, result);
        } else if (criteria instanceof Negation) {
            throw new RuntimeException("negation clause is not supported, please change your query");
        } else {
            throw new UnsupportedOperationException("Queries of type " +
                    criteria.getClass().getSimpleName() + " are not supported by this implementation");
        }

        return result;
    }

    private void handleDisjunction(Junction criteria, List<List<PropertyCriterion>> result) {
        //we flatten each criterion and add output to result
        for (Criterion c : criteria.getCriteria()) {
            if (c instanceof PropertyCriterion) {
                List<PropertyCriterion> temp = new ArrayList<PropertyCriterion>();
                temp.add((PropertyCriterion) c);

                result.add(temp);
            } else {
                List<List<PropertyCriterion>> flattened = flattenAndReplaceDisjunction((Junction) c);
                result.addAll(flattened);
            }
        }
    }

    private List<List<PropertyCriterion>> handleConjunction(Conjunction criterion) {
        List<List<PropertyCriterion>> result = new ArrayList<List<PropertyCriterion>>();
        //first collect the non-disjunctions and disjunctions
        List<PropertyCriterion> properties = new ArrayList<PropertyCriterion>();
        List<List<List<PropertyCriterion>>> toCombinate = new ArrayList<List<List<PropertyCriterion>>>();
        for (Criterion c : ((Conjunction) criterion).getCriteria()) {
            if (c instanceof PropertyCriterion) {
                properties.add((PropertyCriterion) c);
            }
            if (c instanceof Conjunction) {
                //lets process it and see if there were any disjunctions internally
                List<List<PropertyCriterion>> inner = flattenAndReplaceDisjunction((Junction) c);
                if (inner.size() == 1) {
                    properties.addAll(inner.get(0));
                } else {
                    toCombinate.add(inner);
                }
            }
            if (c instanceof Disjunction) {
                List<List<PropertyCriterion>> inner = flattenAndReplaceDisjunction((Junction) c);
                //a or b = [ [a],[b] ]
                toCombinate.add(inner);
            }
            if (c instanceof Negation) {
                throw new RuntimeException("negation clause is not supported, please change your query");
            }
        }

        if (toCombinate.isEmpty()) {
            result.add(properties);
        } else {
            /*
             con((a or b),(c or d),e) =  con(a,c,e),con(b,c,e),con(a,d,e),con(b,d,e)
             con((a or b),(c or d),e) =>
             toCombinate is [
                [ [a],[b] ],
                [ [c],[d] ]
             ]
             properties = [ e ]
             */
            //add properties to the combination list as a single element, so it becomes
            /*
             toCombinate is [
                [ [a],[b] ],
                [ [c],[d] ],
                [ [e] ]
             ]
             */
            List<List<PropertyCriterion>> temp = new ArrayList<List<PropertyCriterion>>();
            temp.add(properties);
            toCombinate.add(temp);

            List<List<PropertyCriterion>> combinations = DynamoDBUtil.combinate(toCombinate);
            result = combinations;
        }
        return result;
    }

    protected Object createObjectFromItem(Map<String, AttributeValue> item) {
        final String id = DynamoDBUtil.getAttributeValue(item, "id");
        return dynamoDBEntityPersister.createObjectFromNativeEntry(getEntity(), id,
                new DynamoDBNativeItem(item));
    }

    protected static interface QueryHandler<T> {
        public void handle(PersistentEntity entity, T criterion, Map<String, Condition> filter);
    }

    protected static String extractPropertyKey(String propertyName, PersistentEntity entity) {
        PersistentProperty prop = null;
        if (entity.isIdentityName(propertyName)) {
            prop = entity.getIdentity();
        } else {
            prop = entity.getPropertyByName(propertyName);
        }

        if (prop == null) {
            throw new IllegalArgumentException(
                    "Could not find property '" + propertyName + "' in entity '" + entity.getName() + "' : " + entity);
        }

        KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
        String key = kv.getKey();
        return key;
    }

    /**
     * Returns mapped key
     *
     * @param entity
     * @param propertyName
     * @return
     */
    protected static String getKey(PersistentEntity entity, String propertyName) {
        return extractPropertyKey(propertyName, entity);
    }

    /**
     * simple temp container
     */
    protected static class Results {
        public Results(List<Object> objects, List<Map<String, AttributeValue>> items) {
            this.objects = objects;
            this.items = items;
        }

        public List<Object> objects;
        public List<Map<String, AttributeValue>> items;
    }
}
