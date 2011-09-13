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
package org.grails.datastore.mapping.query.jpa;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.*;

/**
 * Builds JPA 1.0 String-based queries from the Query model
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaQueryBuilder {
    private static final String DISTINCT_CLAUSE = "DISTINCT ";
    private static final String SELECT_CLAUSE = "SELECT ";
    private static final String AS_CLAUSE = " AS ";
    private static final String FROM_CLAUSE = " FROM ";
    private static final String ORDER_BY_CLAUSE = " ORDER BY ";
    private static final String WHERE_CLAUSE = " WHERE ";
    private static final char COMMA = ',';
    private static final char CLOSE_BRACKET = ')';
    private static final char OPEN_BRACKET = '(';
    private static final char SPACE = ' ';
    private static final char QUESTIONMARK = '?';
    private static final char DOT = '.';
    public static final String NOT_CLAUSE = " NOT";
    public static final String LOGICAL_AND = " AND ";
    public static final String LOGICAL_OR = " OR ";


    private static final Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    private PersistentEntity entity;
    private Query.Junction criteria;
    private Query.ProjectionList projectionList;
    private List<Query.Order> orders;
    private String logicalName;
    private ConversionService conversionService = new GenericConversionService();


    public JpaQueryBuilder(PersistentEntity entity, Query.Junction criteria) {
        this.entity = entity;
        this.criteria = criteria;
        this.logicalName = entity.getDecapitalizedName();
    }

    public JpaQueryBuilder(PersistentEntity entity, Query.Junction criteria, Query.ProjectionList projectionList) {
        this(entity, criteria);
        this.projectionList = projectionList;
    }

    public JpaQueryBuilder(PersistentEntity entity, Query.Junction criteria, Query.ProjectionList projectionList, List<Query.Order> orders) {
        this(entity, criteria, projectionList);
        this.orders = orders;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public JpaQueryInfo buildUpdate(Map propertiesToUpdate) {

        return null;
    }

    public JpaQueryInfo buildDelete() {
        return null;
    }

    public JpaQueryInfo buildSelect() {
        StringBuilder queryString = new StringBuilder(SELECT_CLAUSE);

        if (projectionList.isEmpty()) {
            queryString.append(DISTINCT_CLAUSE)
                    .append(logicalName);
        }
        else {
            for (Iterator i = projectionList.getProjectionList().iterator(); i.hasNext();) {
                Query.Projection projection = (Query.Projection) i.next();
                if (projection instanceof Query.CountProjection) {
                    queryString.append("COUNT(")
                            .append(logicalName)
                            .append(CLOSE_BRACKET);
                }
                else if (projection instanceof Query.IdProjection) {
                    queryString.append(logicalName)
                            .append(DOT)
                            .append(entity.getIdentity().getName());
                }
                else if (projection instanceof Query.PropertyProjection) {
                    Query.PropertyProjection pp = (Query.PropertyProjection) projection;
                    if (projection instanceof Query.AvgProjection) {
                        queryString.append("AVG(")
                                .append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName())
                                .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof Query.SumProjection) {
                        queryString.append("SUM(")
                                .append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName())
                                .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof Query.MinProjection) {
                        queryString.append("MIN(")
                                .append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName())
                                .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof Query.MaxProjection) {
                        queryString.append("MAX(")
                                .append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName())
                                .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof Query.CountDistinctProjection) {
                        queryString.append("COUNT(DISTINCT ")
                                .append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName())
                                .append(CLOSE_BRACKET);
                    }
                    else {
                        queryString.append(logicalName)
                                .append(DOT)
                                .append(pp.getPropertyName());
                    }
                }

                if (i.hasNext()) {
                    queryString.append(COMMA);
                }
            }
        }

        queryString.append(FROM_CLAUSE)
                .append(entity.getName())
                .append(AS_CLAUSE )
                .append(logicalName);

        StringBuilder whereClause= new StringBuilder();
        List parameters = null;
        if (!criteria.isEmpty()) {
            parameters = buildWhereClause(entity, criteria, queryString, whereClause,logicalName);
        }

        appendOrder(queryString, logicalName);
        return new JpaQueryInfo(queryString.toString(), parameters);
    }

    static public int appendCriteriaForOperator(StringBuilder q,
            String logicalName, final String name, int position, String operator) {
        q.append(logicalName)
         .append(DOT)
         .append(name)
         .append(operator)
         .append(QUESTIONMARK)
         .append(++position);
        return position;
    }

    static {
        queryHandlers.put(AssociationQuery.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion,
                    StringBuilder q, StringBuilder whereClause, String logicalName, int position, List parameters,
                    ConversionService conversionService) {

                AssociationQuery aq = (AssociationQuery) criterion;
                final Association<?> association = aq.getAssociation();
                if (association instanceof ToOne) {
                    final String associationName = association.getName();
                    logicalName = logicalName + DOT + associationName;
                    return buildWhereClauseForCriterion(association.getAssociatedEntity(), aq.getCriteria(), q, whereClause, logicalName, aq.getCriteria().getCriteria(), position, parameters, conversionService);
                }

                if (association != null) {
                    final String associationName = association.getName();
                    // TODO: Allow customization of join strategy!
                    q.append(" INNER JOIN ")
                     .append(logicalName)
                     .append(DOT)
                     .append(associationName)
                     .append(SPACE)
                     .append(associationName);

                    return buildWhereClauseForCriterion(association.getAssociatedEntity(), aq.getCriteria(), q, whereClause, associationName, aq.getCriteria().getCriteria(), position, parameters, conversionService);
                }

                return position;
            }
        });

        queryHandlers.put(Query.Negation.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion,
                    StringBuilder q, StringBuilder whereClause, String logicalName, int position, List parameters,
                    ConversionService conversionService) {

                whereClause.append(NOT_CLAUSE)
                           .append(OPEN_BRACKET);

                final Query.Negation negation = (Query.Negation)criterion;
                position = buildWhereClauseForCriterion(entity, negation, q, whereClause, logicalName, negation.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Query.Conjunction.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion,
                    StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters,
                    ConversionService conversionService) {
                whereClause.append(OPEN_BRACKET);

                final Query.Conjunction conjunction = (Query.Conjunction)criterion;
                position = buildWhereClauseForCriterion(entity, conjunction, q, whereClause, logicalName, conjunction.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Query.Disjunction.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion,
                    StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters,
                    ConversionService conversionService) {
                whereClause.append(OPEN_BRACKET);

                final Query.Disjunction disjunction = (Query.Disjunction)criterion;
                position = buildWhereClauseForCriterion(entity, disjunction, q,whereClause,  logicalName, disjunction.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Query.Equals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.Equals eq = (Query.Equals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.Equals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, "=");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

       queryHandlers.put(Query.EqualsProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.EqualsProperty eq = (Query.EqualsProperty) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.EqualsProperty.class);
                validateProperty(entity, otherProperty, Query.EqualsProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, "=");
                return position;
            }
        });

       queryHandlers.put(Query.NotEqualsProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.PropertyComparisonCriterion eq = (Query.PropertyComparisonCriterion) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.NotEqualsProperty.class);
                validateProperty(entity, otherProperty, Query.NotEqualsProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, "!=");
                return position;
            }
        });

       queryHandlers.put(Query.GreaterThanProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.PropertyComparisonCriterion eq = (Query.PropertyComparisonCriterion) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.GreaterThanProperty.class);
                validateProperty(entity, otherProperty, Query.GreaterThanProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, ">");
                return position;
            }
        });

       queryHandlers.put(Query.GreaterThanEqualsProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.PropertyComparisonCriterion eq = (Query.PropertyComparisonCriterion) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.GreaterThanEqualsProperty.class);
                validateProperty(entity, otherProperty, Query.GreaterThanEqualsProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, ">=");
                return position;
            }
        });

       queryHandlers.put(Query.LessThanProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.PropertyComparisonCriterion eq = (Query.PropertyComparisonCriterion) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.LessThanProperty.class);
                validateProperty(entity, otherProperty, Query.LessThanProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, "<");
                return position;
            }
        });

       queryHandlers.put(Query.LessThanEqualsProperty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.PropertyComparisonCriterion eq = (Query.PropertyComparisonCriterion) criterion;
                final String propertyName = eq.getProperty();
                String otherProperty = eq.getOtherProperty();

                validateProperty(entity, propertyName, Query.LessThanEqualsProperty.class);
                validateProperty(entity, otherProperty, Query.LessThanEqualsProperty.class);
                appendPropertyComparison(whereClause, logicalName, propertyName, otherProperty, "<=");
                return position;
            }
        });

        queryHandlers.put(Query.IsNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IsNull isNull = (Query.IsNull) criterion;
                final String name = isNull.getProperty();
                validateProperty(entity, name, Query.IsNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NULL ");

                return position;
            }
        });

        queryHandlers.put(Query.IsNotNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IsNotNull isNotNull = (Query.IsNotNull) criterion;
                final String name = isNotNull.getProperty();
                validateProperty(entity, name, Query.IsNotNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NOT NULL ");

                return position;
            }
        });

        queryHandlers.put(Query.IsEmpty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IsEmpty isEmpty = (Query.IsEmpty) criterion;
                final String name = isEmpty.getProperty();
                validateProperty(entity, name, Query.IsEmpty.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS EMPTY ");

                return position;
            }
        });

        queryHandlers.put(Query.IsNotEmpty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IsNotEmpty isNotEmpty = (Query.IsNotEmpty) criterion;
                final String name = isNotEmpty.getProperty();
                validateProperty(entity, name, Query.IsNotEmpty.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS EMPTY ");

                return position;
            }
        });

        queryHandlers.put(Query.IsNotNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q,StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IsNotNull isNotNull = (Query.IsNotNull) criterion;
                final String name = isNotNull.getProperty();
                validateProperty(entity, name, Query.IsNotNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NOT NULL ");

                return position;
            }
        });

        queryHandlers.put(Query.IdEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.IdEquals eq = (Query.IdEquals) criterion;
                PersistentProperty prop = entity.getIdentity();
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, prop.getName(), position, "=");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.NotEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.NotEquals eq = (Query.NotEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.NotEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " != ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.GreaterThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.GreaterThan eq = (Query.GreaterThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.GreaterThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " > ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.LessThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.LessThanEquals eq = (Query.LessThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.LessThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " <= ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.GreaterThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.GreaterThanEquals eq = (Query.GreaterThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.GreaterThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " >= ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.Between.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.Between between = (Query.Between) criterion;
                final Object from = between.getFrom();
                final Object to = between.getTo();

                final String name = between.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.Between.class);
                Class propType = prop.getType();
                final String qualifiedName = logicalName + DOT + name;
                whereClause.append(OPEN_BRACKET)
                           .append(qualifiedName)
                           .append(" >= ")
                           .append(QUESTIONMARK)
                           .append(++position)
                           .append(" AND ")
                           .append(qualifiedName)
                           .append(" <= ")
                           .append(QUESTIONMARK)
                           .append(++position)
                           .append(CLOSE_BRACKET);

                parameters.add(conversionService.convert( from, propType ));
                parameters.add(conversionService.convert( to, propType ));
                return position;
            }
        });

        queryHandlers.put(Query.LessThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.LessThan eq = (Query.LessThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.LessThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " < ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.Like.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.Like eq = (Query.Like) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.Like.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " like ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.ILike.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.ILike eq = (Query.ILike) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.ILike.class);
                Class propType = prop.getType();
                q.append("lower(")
                 .append(logicalName)
                 .append(DOT)
                 .append(name)
                 .append(")")
                 .append(" like lower(")
                 .append(QUESTIONMARK)
                 .append(")")
                 .append(++position);
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Query.In.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Query.In eq = (Query.In) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Query.In.class);
                Class propType = prop.getType();
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IN (");
                for (Iterator i = eq.getValues().iterator(); i.hasNext();) {
                    Object val = i.next();
                    whereClause.append(QUESTIONMARK)
                               .append(++position);
                    if (i.hasNext()) {
                        whereClause.append(COMMA);
                    }
                    parameters.add(conversionService.convert(val, propType));
                }
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });
    }

    private static void appendPropertyComparison(StringBuilder q, String logicalName, String propertyName, String otherProperty, String operator) {
        q.append(logicalName)
         .append(DOT)
         .append(propertyName)
         .append(operator)
         .append(logicalName)
         .append(DOT)
         .append(otherProperty);
    }


    private static PersistentProperty validateProperty(PersistentEntity entity, String name, Class criterionType) {
        if (entity.getIdentity().getName().equals(name)) return entity.getIdentity();
        PersistentProperty prop = entity.getPropertyByName(name);
        if (prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use [" +
                  criterionType.getSimpleName() + "] criterion on non-existent property: " + name);
        }
        return prop;
    }

    private static interface QueryHandler {
        public int handle(PersistentEntity entity, Query.Criterion criterion, StringBuilder q, StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService);
    }

    private List buildWhereClause(PersistentEntity entity, Query.Junction criteria, StringBuilder q, StringBuilder whereClause, String logicalName) {
        final List<Query.Criterion> criterionList = criteria.getCriteria();
        whereClause.append(WHERE_CLAUSE);
        if (criteria instanceof Query.Negation) {
            whereClause.append(NOT_CLAUSE);
        }
        whereClause.append(OPEN_BRACKET);
        int position = 0;
        List parameters = new ArrayList();
        position = buildWhereClauseForCriterion(entity, criteria, q, whereClause, logicalName,
                criterionList, position, parameters,
                conversionService);
        q.append(whereClause.toString());
        q.append(CLOSE_BRACKET);
        return parameters;
    }

    protected void appendOrder(StringBuilder queryString, String logicalName) {
        if (!orders.isEmpty()) {
            queryString.append( ORDER_BY_CLAUSE);
            for (Query.Order order : orders) {
                queryString.append(logicalName)
                           .append(DOT)
                           .append(order.getProperty())
                           .append(SPACE)
                           .append(order.getDirection().toString())
                           .append(SPACE);
            }
        }
    }

    static int buildWhereClauseForCriterion(PersistentEntity entity,
            Query.Junction criteria, StringBuilder q, StringBuilder whereClause, String logicalName,
            final List<Query.Criterion> criterionList, int position, List parameters, ConversionService conversionService) {
        for (Iterator<Query.Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Query.Criterion criterion = iterator.next();

            final String operator = criteria instanceof Query.Conjunction ? LOGICAL_AND : LOGICAL_OR;
            QueryHandler qh = queryHandlers.get(criterion.getClass());
            if (qh != null) {
                position = qh.handle(entity, criterion, q, whereClause, logicalName,
                        position, parameters, conversionService);
            }
            else {
                throw new InvalidDataAccessResourceUsageException("Queries of type "+criterion.getClass().getSimpleName()+" are not supported by this implementation");
            }

            if (iterator.hasNext()) {
                whereClause.append(operator);
            }
        }

        return position;
    }

}
