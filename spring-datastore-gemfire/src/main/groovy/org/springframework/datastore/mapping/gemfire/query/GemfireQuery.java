/* Copyright (C) 2010 SpringSource
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
package org.springframework.datastore.mapping.gemfire.query;

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.cache.query.SelectResults;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.datastore.mapping.gemfire.GemfireDatastore;
import org.springframework.datastore.mapping.gemfire.GemfireSession;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.ToOne;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.query.order.ManualEntityOrdering;
import org.springframework.datastore.mapping.query.projections.ManualProjections;

import java.util.*;

/**
 * <p>Adds query support for Gemfire. Note that due to limitations in the Gemfire API some operations
 * are more expensive than in other stores.</p>
 *
 * <p>In particular Gemfire doesn't support native ORDER BY or OFFSET clauses hence these are handled manually by this
 * implementation which results in some performance implications when ordering or offsets are used in queries.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class GemfireQuery extends Query {
    public static final String SELECT_CLAUSE = "SELECT ";
    public static final String SELECT_DISTINCT = " DISTINCT ";
    public static final String WHERE_CLAUSE = " WHERE ";
    public static final String FROM_CLAUSE = " FROM /";
    public static final String LIMIT_CLAUSE = " LIMIT ";
    public static final String NOT_CLAUSE = " NOT ";
    public static final String LOGICAL_OR = " OR ";
    private ManualEntityOrdering ordering;
    private ManualProjections manualProjections;
    private String regionName;

    private static interface QueryHandler {
        public int handle(PersistentEntity entity, Criterion criterion, StringBuilder query, List params, int index);
    }

    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();

    public static final String GREATER_THAN_EQUALS = " >= ";

    public static final String LESS_THAN_EQUALS = " <= ";

    public static final String LOGICAL_AND = " AND ";

    public static final String GREATER_THAN = " > ";

    public static final String WILDCARD = " * ";

    public static final char SPACE = ' ';

    public static final char DOLLAR_SIGN = '$';

    public static final String LESS_THAN = " < ";

    public static final String EQUALS = " = ";

    public static final String NOT_EQUALS = " != ";

    public static final String LIKE = " like ";

    static {
        queryHandlers.put(Equals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                Equals eq = (Equals) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, Equals.class);


                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), EQUALS);
            }
        });
        queryHandlers.put(IdEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
            	IdEquals eq = (IdEquals) criterion;
                String name = entity.getIdentity().getName();
                validateProperty(entity, name, Equals.class);


                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), EQUALS);
            }
        });        
        queryHandlers.put(NotEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                NotEquals eq = (NotEquals) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, NotEquals.class);

                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), NOT_EQUALS);
            }
        });
        queryHandlers.put(Like.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                Like eq = (Like) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, Like.class);
                q.append(calculateName(entity, name))
                 .append(" like '")
                 .append(eq.getValue())
                 .append("' ");

//                params.add(eq.getValue());
                return index;
            }
        });

        queryHandlers.put(In.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                In eq = (In) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, In.class);
                q.append(calculateName(entity, name))
                 .append(" IN SET (");

                final Collection values = eq.getValues();
                for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                    Object value = iterator.next();
                    index = appendOrEmbedValue(q, params, index, value, "");
                    if(iterator.hasNext()) q.append(",");
                }

                q.append(") ");

                return index;
            }
        });
        queryHandlers.put(Disjunction.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder query, List params, int index) {
                return buildWhereClause(entity, (Junction) criterion,query, index, params);
            }
        });
        queryHandlers.put(Conjunction.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder query, List params, int index) {
                return buildWhereClause(entity, (Junction) criterion,query, index, params);
            }
        });
        queryHandlers.put(Negation.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder query, List params, int index) {
                return buildWhereClause(entity, (Junction) criterion,query, index, params);
            }
        });

        queryHandlers.put(Between.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                Between eq = (Between) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, Between.class);
                q.append("(");
                final String calculatedName = calculateName(entity, name);
                q.append(calculatedName);

                index = appendOrEmbedValue(q, params, index, eq.getFrom(), GREATER_THAN_EQUALS);
                q.append(LOGICAL_AND)
                 .append(calculatedName);

                index = appendOrEmbedValue(q, params, index, eq.getTo(), LESS_THAN_EQUALS);
                q.append(") ");

                return index;
            }
        });


        queryHandlers.put(GreaterThan.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                PropertyCriterion eq = (PropertyCriterion) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, GreaterThan.class);
                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), GREATER_THAN);
            }
        });

       queryHandlers.put(GreaterThanEquals.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                PropertyCriterion eq = (PropertyCriterion) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, GreaterThanEquals.class);
                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), GREATER_THAN_EQUALS);
            }
        });

       queryHandlers.put(LessThanEquals.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                PropertyCriterion eq = (PropertyCriterion) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, LessThanEquals.class);
                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), LESS_THAN_EQUALS);
            }
        });

        queryHandlers.put(LessThan.class, new QueryHandler() {

            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, List params, int index) {
                PropertyCriterion eq = (PropertyCriterion) criterion;
                final String name = eq.getProperty();
                validateProperty(entity, name, LessThan.class);
                q.append(calculateName(entity, name));
                return appendOrEmbedValue(q, params, index, eq.getValue(), LESS_THAN);
            }
        });

    }

    private static int appendOrEmbedValue(StringBuilder q, List params, int index, Object value, String operator) {
        if(params == null) {
            q.append(operator);
            appendValue(q, value);
            q.append(SPACE);
        }
        else {
            q.append(operator)
             .append(DOLLAR_SIGN)
             .append(++index)
             .append(SPACE);
            params.add(value);
        }
        return index;
    }

    private static void appendValue(StringBuilder q, Object value) {
        if(value instanceof Number || value instanceof Boolean) {
            q.append(value);
        }
        else {
            q.append(quote(value));
        }
    }

    private static Object quote(Object value) {
        return "'" + value + "'";
    }

    private static String calculateName(PersistentEntity entity, String name) {
        final PersistentProperty prop = entity.getPropertyByName(name);
        if(prop instanceof ToOne) {
            ToOne association = (ToOne) prop;
            final PersistentEntity associated = association.getAssociatedEntity();
            return name + "." + associated.getIdentity().getName();
        }
        return name;
    }

    private static void validateProperty(PersistentEntity entity, String name, Class criterionType) {
        if(entity.getIdentity().getName().equals(name)) return;
        PersistentProperty prop = entity.getPropertyByName(name);
        if(prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use ["+ criterionType.getSimpleName()+"] criterion on non-existent property: " + name);
        }
    }

    GemfireDatastore gemfireDatastore;

    public GemfireQuery(GemfireSession session, PersistentEntity entity) {
        super(session, entity);
        gemfireDatastore = (GemfireDatastore) session.getDatastore();
        this.ordering = new ManualEntityOrdering(entity);
        this.manualProjections = new ManualProjections(entity);
        final org.springframework.datastore.mapping.gemfire.config.Region region = getMappedRegionInfo(entity);

        if(region != null && region.getRegion() != null) {
            this.regionName = region.getRegion();
        }
        else {
            this.regionName = entity.getDecapitalizedName();
        }

    }

    private org.springframework.datastore.mapping.gemfire.config.Region getMappedRegionInfo(PersistentEntity entity) {
        final Object mappedForm = entity.getMapping().getMappedForm();

        org.springframework.datastore.mapping.gemfire.config.Region mappedRegion = null;
        if(mappedForm instanceof org.springframework.datastore.mapping.gemfire.config.Region)
            mappedRegion = (org.springframework.datastore.mapping.gemfire.config.Region) mappedForm;
        return mappedRegion;
    }

    @Override
    protected List executeQuery(final PersistentEntity entity, Junction criteria) {
        final ProjectionList projectionList = projections();
        if(criteria.isEmpty() && !(max > -1)) {
            return (List) gemfireDatastore.getTemplate(entity).execute(new GemfireCallback() {

                public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {

                    List finalResults;
                    if(projectionList.isEmpty()) {
                        finalResults = new ArrayList(region.values());
                    }
                    else {
                        List results = new ArrayList();
                        for (Projection projection : projectionList.getProjectionList()) {
                            Collection values = null;
                            if(projection instanceof CountProjection) {
                                results.add(region.size());
                            }
                            else if(projection instanceof MinProjection) {
                                MinProjection min = (MinProjection) projection;
                                if(values == null) {
                                    values = region.values();
                                }
                                results.add(manualProjections.min(values, min.getPropertyName()));
                            }
                            else if(projection instanceof MaxProjection) {
                                if(values == null) {
                                    values = region.values();
                                }
                                MaxProjection maxProjection = (MaxProjection) projection;
                                results.add(manualProjections.max(values,maxProjection.getPropertyName()));
                            }
                            else if(projection instanceof IdProjection) {
                                results.add(region.keySet());
                            }
                            else if(projection.getClass() ==  PropertyProjection.class) {
                                if(values == null) {
                                    values = region.values();
                                }
                                final List propertyProjectionResults = manualProjections.property(values, ((PropertyProjection) projection).getPropertyName());
                                if(projectionList.getProjectionList().size() == 1) {
                                    results = propertyProjectionResults;
                                }
                                else {
                                    results.add(propertyProjectionResults);
                                }
                            }

                        }
                        finalResults = results;
                    }
                    finalResults = ordering.applyOrder(finalResults, getOrderBy());
                    return finalResults;
                }
            });
        }
        else {
            GemfireTemplate template = gemfireDatastore.getTemplate(entity);
            final List params = new ArrayList();
            final String queryString = getQueryString(params, true);

            return (List) template.execute(new GemfireCallback() {

                public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {

                    final QueryService queryService = gemfireDatastore.getGemfireCache().getQueryService();

                    final com.gemstone.gemfire.cache.query.Query gemfireQuery = queryService.newQuery(queryString);

                    final Object result = gemfireQuery.execute(params.toArray());
                    List finalResults = Collections.emptyList();
                    if(projectionList.isEmpty()) {
                        if(result instanceof SelectResults) {
                            finalResults = ((SelectResults)result).asList();
                        }
                        else {
                            finalResults = wrapResultInList(result);
                        }
                    }
                    else {
                        if(result instanceof SelectResults) {
                            SelectResults selectResults = (SelectResults) result;
                            finalResults = applyProjections(selectResults.asList(), projectionList);
                        }
                        else {
                            finalResults = wrapResultInList(result);
                        }

                    }

                    finalResults = ordering.applyOrder(finalResults, getOrderBy());

                    if(offset > 0 ) {

                        final int resultSize = finalResults.size();
                        if(offset > resultSize) {
                            finalResults = Collections.emptyList();
                        }
                        else {
                            int end = resultSize;
                            if(max > -1) {
                                end = offset + max;
                                if(end > resultSize) {
                                    end = resultSize;
                                }
                            }
                            finalResults = finalResults.subList(offset, end);
                        }
                    }

                    return finalResults;
                }
            });
        }
    }

    /**
     * Obtains the query string with variables embedded within the Query
     * @return The query string
     */
    public String getQueryString() {
        return getQueryString(null, false);
    }

    protected String getQueryString(List params, boolean distinct) {
        ProjectionList projectionList = projections();
        String select = SELECT_CLAUSE;
        String from = FROM_CLAUSE + regionName;
        String where = WHERE_CLAUSE;


        final StringBuilder q = new StringBuilder();
        q.append(select);
        if(distinct)
            q.append(SELECT_DISTINCT);
        if(projectionList.isEmpty()) {
            q.append(WILDCARD);
        }
        else {
            boolean modifiedQuery = false;
            for (Projection projection : projectionList.getProjectionList()) {
                if(projection instanceof IdProjection) {
                    if(modifiedQuery) {
                        q.append(',');
                    }
                    q.append(SPACE).append(entity.getIdentity().getName());
                    modifiedQuery = true;

                }
                else if(projection.getClass() == PropertyProjection.class) {
                    if(modifiedQuery) {
                        q.append(',');
                    }

                    q.append(SPACE).append(((PropertyProjection)projection).getPropertyName());
                    modifiedQuery = true;
                }
            }

            if(!modifiedQuery) {
                q.append(WILDCARD);
            }
            else {
                q.append(SPACE);
            }
        }
        q.append(from);

        if(!criteria.isEmpty()) {
            q.append(where);
            buildWhereClause(entity, criteria, q, 0, params);
        }

        if(max > 0 && offset == 0) {
            q.append(LIMIT_CLAUSE).append(max);
        }

        return q.toString();
    }

    private List applyProjections(List results, ProjectionList projections) {
        List projectedResults = new ArrayList();
        for (Projection projection : projections.getProjectionList()) {
            if(projection instanceof CountProjection) {
                projectedResults.add(results.size());
            }
            else if(projection instanceof MinProjection) {
                MinProjection min = (MinProjection) projection;
                projectedResults.add(manualProjections.min(results,min.getPropertyName()));
            }
           else if(projection instanceof MaxProjection) {
                MaxProjection min = (MaxProjection) projection;
                projectedResults.add(manualProjections.max(results,min.getPropertyName()));
            }
        }
        if(projectedResults.isEmpty()) {
        	return results;
        }
        return projectedResults;
    }


    private List wrapResultInList(Object result) {
        List listResult = new ArrayList();
        listResult.add(result);
        return listResult;
    }




    private static int buildWhereClause(PersistentEntity entity, Junction criteria, StringBuilder q, int index, List params) {
        final List<Criterion> criterionList = criteria.getCriteria();
        if(criteria instanceof Negation) {
            q.append(NOT_CLAUSE);
        }
        q.append('(');
        for (Iterator<Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Criterion criterion = iterator.next();

            final String operator = criteria instanceof Conjunction ? LOGICAL_AND : LOGICAL_OR;
            QueryHandler qh = queryHandlers.get(criterion.getClass());
            if(qh != null) {
                index = qh.handle(entity,criterion, q, params, index);
            }

            if(iterator.hasNext())
                q.append(operator);

        }
        q.append(')');
        return index;
    }
}
