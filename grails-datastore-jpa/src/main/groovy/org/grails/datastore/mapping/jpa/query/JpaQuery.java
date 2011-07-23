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

package org.grails.datastore.mapping.jpa.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.grails.datastore.mapping.jpa.JpaSession;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Restrictions;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * Query implementation for JPA.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"hiding", "rawtypes", "unchecked"})
public class JpaQuery extends Query {

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

    private static final Log LOG = LogFactory.getLog(org.grails.datastore.mapping.jpa.query.JpaQuery.class);
    private static final Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();

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
            public int handle(PersistentEntity entity, Criterion criterion,
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

        queryHandlers.put(Negation.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion,
                    StringBuilder q, StringBuilder whereClause, String logicalName, int position, List parameters,
                    ConversionService conversionService) {

                whereClause.append(NOT_CLAUSE)
                           .append(OPEN_BRACKET);

                final Negation negation = (Negation)criterion;
                position = buildWhereClauseForCriterion(entity, negation, q, whereClause, logicalName, negation.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Conjunction.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion,
                    StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters,
                    ConversionService conversionService) {
                whereClause.append(OPEN_BRACKET);

                final Conjunction conjunction = (Conjunction)criterion;
                position = buildWhereClauseForCriterion(entity, conjunction, q, whereClause, logicalName, conjunction.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Disjunction.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion,
                    StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters,
                    ConversionService conversionService) {
                whereClause.append(OPEN_BRACKET);

                final Disjunction disjunction = (Disjunction)criterion;
                position = buildWhereClauseForCriterion(entity, disjunction, q,whereClause,  logicalName, disjunction.getCriteria(), position, parameters, conversionService);
                whereClause.append(CLOSE_BRACKET);

                return position;
            }
        });

        queryHandlers.put(Equals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Equals eq = (Equals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Equals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, "=");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(IsNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IsNull isNull = (IsNull) criterion;
                final String name = isNull.getProperty();
                validateProperty(entity, name, IsNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NULL ");

                return position;
            }
        });

        queryHandlers.put(IsNotNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IsNotNull isNotNull = (IsNotNull) criterion;
                final String name = isNotNull.getProperty();
                validateProperty(entity, name, IsNotNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NOT NULL ");

                return position;
            }
        });

        queryHandlers.put(IsEmpty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IsEmpty isEmpty = (IsEmpty) criterion;
                final String name = isEmpty.getProperty();
                validateProperty(entity, name, IsEmpty.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS EMPTY ");

                return position;
            }
        });

        queryHandlers.put(IsNotEmpty.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IsNotEmpty isNotEmpty = (IsNotEmpty) criterion;
                final String name = isNotEmpty.getProperty();
                validateProperty(entity, name, IsNotEmpty.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS EMPTY ");

                return position;
            }
        });

        queryHandlers.put(IsNotNull.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q,StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IsNotNull isNotNull = (IsNotNull) criterion;
                final String name = isNotNull.getProperty();
                validateProperty(entity, name, IsNotNull.class);
                whereClause.append(logicalName)
                           .append(DOT)
                           .append(name)
                           .append(" IS NOT NULL ");

                return position;
            }
        });

        queryHandlers.put(IdEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                IdEquals eq = (IdEquals) criterion;
                PersistentProperty prop = entity.getIdentity();
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, prop.getName(), position, "=");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(NotEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                NotEquals eq = (NotEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, NotEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " != ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(GreaterThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                GreaterThan eq = (GreaterThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, GreaterThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " > ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(LessThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                LessThanEquals eq = (LessThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, LessThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " <= ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(GreaterThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService) {
                GreaterThanEquals eq = (GreaterThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, GreaterThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " >= ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Between.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Between between = (Between) criterion;
                final Object from = between.getFrom();
                final Object to = between.getTo();

                final String name = between.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Between.class);
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

        queryHandlers.put(LessThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q,StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService) {
                LessThan eq = (LessThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, LessThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " < ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(Like.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                Like eq = (Like) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Like.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(whereClause, logicalName, name, position, " like ");
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });

        queryHandlers.put(In.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause,String logicalName, int position, List parameters, ConversionService conversionService) {
                In eq = (In) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, In.class);
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

    private static PersistentProperty validateProperty(PersistentEntity entity, String name, Class criterionType) {
        if (entity.getIdentity().getName().equals(name)) return entity.getIdentity();
        PersistentProperty prop = entity.getPropertyByName(name);
        if (prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use [" +
                  criterionType.getSimpleName() + "] criterion on non-existent property: " + name);
        }
        return prop;
    }

    public JpaQuery(JpaSession session, PersistentEntity entity) {
        super(session, entity);

        if (session == null) {
            throw new InvalidDataAccessApiUsageException("Argument session cannot be null");
        }
        if (entity == null) {
            throw new InvalidDataAccessApiUsageException("No persistent entity specified");
        }
    }

    @Override
    public JpaSession getSession() {
        return (JpaSession) super.getSession();
    }

    @Override
    public void add(Criterion criterion) {
        if (criterion instanceof Equals) {
            final Equals eq = (Equals) criterion;
            Object resolved = resolveIdIfEntity(eq.getValue());
            if (resolved != eq.getValue()) {
                criterion = Restrictions.idEq(resolved);
            }
        }

        criteria.add(criterion);
    }

    @Override
    protected List executeQuery(final PersistentEntity entity, final Junction criteria) {
        final JpaTemplate jpaTemplate = getSession().getJpaTemplate();

        return (List)jpaTemplate.execute(new JpaCallback<Object>() {
            public Object doInJpa(EntityManager em) throws PersistenceException {
                return executeQuery(entity, criteria, em, false);
            }
        });
    }

    @Override
    public Object singleResult() {
        final JpaTemplate jpaTemplate = getSession().getJpaTemplate();
        try {
            return jpaTemplate.execute(new JpaCallback<Object>() {
                public Object doInJpa(EntityManager em) throws PersistenceException {
                    return executeQuery(entity, criteria, em, true);
                }
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    protected void appendOrder(StringBuilder queryString, String logicalName) {
        if (!orderBy.isEmpty()) {
            queryString.append( ORDER_BY_CLAUSE);
            for (Order order : orderBy) {
                queryString.append(logicalName)
                           .append(DOT)
                           .append(order.getProperty())
                           .append(SPACE)
                           .append(order.getDirection().toString())
                           .append(SPACE);
            }
        }
    }

    private static interface QueryHandler {
        public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, StringBuilder whereClause, String logicalName, int position, List parameters, ConversionService conversionService);
    }

    private List buildWhereClause(PersistentEntity entity, Junction criteria, StringBuilder q, StringBuilder whereClause, String logicalName) {
        final List<Criterion> criterionList = criteria.getCriteria();
        whereClause.append(WHERE_CLAUSE);
        if (criteria instanceof Negation) {
            whereClause.append(NOT_CLAUSE);
        }
        whereClause.append(OPEN_BRACKET);
        int position = 0;
        List parameters = new ArrayList();
        position = buildWhereClauseForCriterion(entity, criteria, q, whereClause, logicalName,
                criterionList, position, parameters,
                getSession().getMappingContext().getConversionService());
        q.append(whereClause.toString());
        q.append(CLOSE_BRACKET);
        return parameters;
    }

    Object executeQuery(final PersistentEntity entity, final Junction criteria,
            EntityManager em, boolean singleResult) {
        final String logicalName = entity.getDecapitalizedName();
        StringBuilder queryString = new StringBuilder(SELECT_CLAUSE);

        if (projections.isEmpty()) {
            queryString.append(DISTINCT_CLAUSE)
                       .append(logicalName);
        }
        else {
            for (Iterator i = projections.getProjectionList().iterator(); i.hasNext();) {
                Projection projection = (Projection) i.next();
                if (projection instanceof CountProjection) {
                    queryString.append("COUNT(")
                               .append(logicalName)
                               .append(CLOSE_BRACKET);
                }
                else if (projection instanceof IdProjection) {
                    queryString.append(logicalName)
                               .append(DOT)
                               .append(entity.getIdentity().getName());
                }
                else if (projection instanceof PropertyProjection) {
                    PropertyProjection pp = (PropertyProjection) projection;
                    if (projection instanceof AvgProjection) {
                        queryString.append("AVG(")
                                   .append(logicalName)
                                   .append(DOT)
                                   .append(pp.getPropertyName())
                                   .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof SumProjection) {
                        queryString.append("SUM(")
                                   .append(logicalName)
                                   .append(DOT)
                                   .append(pp.getPropertyName())
                                   .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof MinProjection) {
                        queryString.append("MIN(")
                                   .append(logicalName)
                                   .append(DOT)
                                   .append(pp.getPropertyName())
                                   .append(CLOSE_BRACKET);
                    }
                    else if (projection instanceof MaxProjection) {
                        queryString.append("MAX(")
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

        List parameters = null;
        StringBuilder whereClause= new StringBuilder();
        if (!criteria.isEmpty()) {
            parameters = buildWhereClause(entity, criteria, queryString, whereClause,logicalName);
        }

        appendOrder(queryString, logicalName);
        final String queryToString = queryString.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Built JPQL to execute: " + queryToString);
        }
        final javax.persistence.Query q = em.createQuery(queryToString);

        if (parameters != null) {
            for (int i = 0, count = parameters.size(); i < count; i++) {
                q.setParameter(i + 1, parameters.get(i));
            }
        }
        q.setFirstResult(offset);
        if (max > -1) {
            q.setMaxResults(max);
        }

        if (!singleResult) {
            return q.getResultList();
        }
        return q.getSingleResult();
    }

    static int buildWhereClauseForCriterion(PersistentEntity entity,
            Junction criteria, StringBuilder q, StringBuilder whereClause, String logicalName,
            final List<Criterion> criterionList, int position, List parameters, ConversionService conversionService) {
        for (Iterator<Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Criterion criterion = iterator.next();

            final String operator = criteria instanceof Conjunction ? LOGICAL_AND : LOGICAL_OR;
            QueryHandler qh = queryHandlers.get(criterion.getClass());
            if (qh != null) {
                position = qh.handle(entity, criterion, q, whereClause, logicalName,
                        position, parameters, conversionService);
            }

            if (iterator.hasNext()) {
                whereClause.append(operator);
            }
        }

        return position;
    }
}
