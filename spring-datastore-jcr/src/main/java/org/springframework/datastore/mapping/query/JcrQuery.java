package org.springframework.datastore.mapping.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jcr.JcrSession;
import org.springframework.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.extensions.jcr.JcrTemplate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.*;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrQuery extends Query {
    private JcrEntityPersister entityPersister;
    private JcrTemplate jcrTemplate;
    private ConversionService conversionService;

    //    "/jcr:root/nodes//element(ex:nodeName,my:type)"


    public static String DEFAULT_NODE_TYPE = "nt:unstructured";

    //public static String ROOT_NODE = "/jcr:root/nodes//element";

    public static String ROOT_NODE = "//";


    public static final String GREATER_THAN_EQUALS = " >= ";

    public static final String LESS_THAN_EQUALS = " <= ";

    public static final String LOGICAL_AND = " and ";

    public static final String GREATER_THAN = " > ";

    public static final String WILDCARD = " * ";

    public static final char SPACE = ' ';

    public static final char AT_SIGN = '@';

    public static final String LESS_THAN = " < ";

    public static final String EQUALS = " = ";

    public static final String NOT_EQUALS = " != ";

    public static final String LIKE = " [jcr:like] ";

    public static final String ASCENDING = "ascending";

    public static final String DESCENDING = "descending";

    public static final String LIMIT_CLAUSE = " LIMIT ";

    public static final String NOT_CLAUSE = " NOT ";

    public static final String LOGICAL_OR = " or ";


    public JcrQuery(JcrSession session, JcrTemplate jcrTemplate, PersistentEntity persistentEntity, JcrEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        this.jcrTemplate = jcrTemplate;
        this.conversionService = getSession().getMappingContext().getConversionService();
    }

    protected JcrQuery(Session session, PersistentEntity entity) {
        super(session, entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        System.out.println("executeQuery");
        final ProjectionList projectionList = projections();
        List<String> uuids = new ArrayList<String>();
        List finalResults = null;

        if (criteria.isEmpty() && !(max != -1)) {
            //List finalResults = null;
            final String queryString = getQueryString();
            QueryResult qr = jcrTemplate.query(queryString.toString(), javax.jcr.query.Query.XPATH);
            try {
                NodeIterator itr = qr.getNodes();
                while (itr.hasNext()) {
                    Node node = itr.nextNode();
                    uuids.add(node.getUUID());
                }
            } catch (RepositoryException e) {
                throw new InvalidDataAccessResourceUsageException("Cannot execute query. Entity [" + getEntity() + "] does not exist in the repository");
            }            finalResults = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            if (projectionList.isEmpty()) {
                return finalResults;
            } else {
                List results = new ArrayList();
                for (Projection projection : projectionList.getProjectionList()) {
                    final String projectionType = projection.getClass().getSimpleName();
                    Collection values = null;
                    if (projection instanceof CountProjection) {
                        System.out.println("count projection1");
                        
                        results.add(finalResults.size());
                    } else if (projection instanceof MinProjection) {
                        MinProjection min = (MinProjection) projection;
                        return unsupportedProjection(projectionType);

                    } else if (projection instanceof MaxProjection) {
                        return unsupportedProjection(projectionType);

                    } else if (projection instanceof IdProjection) {
                        results.add(uuids);
                    } else if (projection.getClass() == PropertyProjection.class) {
                        return unsupportedProjection(projectionType);
                    }
                }
                finalResults = results;
            }
        } else {
            final List params = new ArrayList();
            final String queryString = getQueryString(params, true);
            QueryResult qr = jcrTemplate.query(queryString.toString(), javax.jcr.query.Query.XPATH);
            try {
                NodeIterator itr = qr.getNodes();
                while (itr.hasNext()) {
                    Node node = itr.nextNode();
                    uuids.add(node.getUUID());
                }
            } catch (RepositoryException e) {
                throw new InvalidDataAccessResourceUsageException("Cannot execute query. Entity [" + getEntity() + "] does not exist in the repository");
            }
            if (uuids.isEmpty())
                return Collections.emptyList();
            else {
                finalResults = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
                IdProjection idProjection = null;
                if (!projectionList.isEmpty()) {
                    List projectionResults = new ArrayList();
                    for (Projection projection : projectionList.getProjectionList()) {
                        final String projectionType = projection.getClass().getSimpleName();
                        if (projection instanceof CountProjection) {
                            System.out.println("count projection2");
                            projectionResults.add(finalResults.size());
                        } else if (projection instanceof MaxProjection) {
                            MaxProjection max = (MaxProjection) projection;
                            return unsupportedProjection(projectionType);
                        } else if (projection instanceof MinProjection) {
                            MinProjection min = (MinProjection) projection;
                            return unsupportedProjection(projectionType);
                        } else {
                            if (projection instanceof SumProjection) {
                                return unsupportedProjection(projectionType);
                            } else if (projection instanceof AvgProjection) {
                                return unsupportedProjection(projectionType);
                            } else if (projection instanceof PropertyProjection) {
                                PropertyProjection propertyProjection = (PropertyProjection) projection;
                                final String propName = propertyProjection.getPropertyName();
                                PersistentProperty prop = entityPersister.getPersistentEntity().getPropertyByName(propName);
                                return unsupportedProjection(projectionType);
                            } else if (projection instanceof IdProjection) {
                                idProjection = (IdProjection) projection;
                            }
                        }
                    }
                     if (!projectionResults.isEmpty()) {
                        return projectionResults;
                      } else if (idProjection != null) {
                          return uuids;
                      } 
                }
            }
            final int total = finalResults.size();
            if (offset > total) {
                finalResults = Collections.emptyList();
            } else {
                int from = offset;
                int to = max == -1 ? -1 : (offset + max) - 1;
                if (to >= total) to = -1;
                if (max != -1) {
                    finalResults = finalResults.subList(from, max);
                }
            }
         }
        return finalResults;

    }

    protected String getQueryString(List params, boolean distinct) {
        ProjectionList projectionList = projections();
        final StringBuilder q = new StringBuilder();
        q.append(ROOT_NODE);
        //q.append("(");
        q.append(getEntity().getJavaClass().getSimpleName());
        // q.append(",");
        //q.append(DEFAULT_NODE_TYPE);
        // q.append(")");
        /*if(distinct)
            q.append(SELECT_DISTINCT);
        if(projectionList.isEmpty()) {
            q.append(WILDCARD);
        }
      */
        //TODO Re-implement this to support XPATH
        if (!projectionList.isEmpty()) {
            boolean modifiedQuery = false;
            for (Projection projection : projectionList.getProjectionList()) {

                if (projection.getClass() == PropertyProjection.class) {
                    if (modifiedQuery) {
                        q.append(',');
                    }

                    q.append(SPACE).append(((PropertyProjection) projection).getPropertyName());
                    modifiedQuery = true;
                }
            }
        }
        //*[jcr:like( @title, '%Ho%' )]
        if (!criteria.isEmpty()) {
            q.append("[");
            buildCondition(entity, criteria, q, 0, params);
            q.append("]");
        }

         for (Order order : orderBy) {
            String direction = null;
            if (order.getDirection().equals(Order.Direction.ASC))
                direction = ASCENDING;
            else
                direction = DESCENDING;
                q.append(SPACE);
                q.append("order by @")
                    .append(order.getProperty())
                    .append(" ")
                    .append(direction);

                }

        System.out.println("querystring: " + q.toString());
        return q.toString();
    }


    private  int buildCondition(PersistentEntity entity, Junction criteria, StringBuilder q, int index, List params) {
        final List<Criterion> criterionList = criteria.getCriteria();
        if (criteria instanceof Negation) {
            q.append(NOT_CLAUSE);
        }
        for (Iterator<Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Criterion criterion = iterator.next();
            final String operator = criteria instanceof Conjunction ? LOGICAL_AND : LOGICAL_OR;
            CriterionHandler qh = criterionHandlers.get(criterion.getClass());
            if (qh != null) {
                qh.handle(entity, criterion, q, params);
            }

            if (iterator.hasNext())
                q.append(operator);

        }
        return index;
    }

    private static interface CriterionHandler<T> {
        void handle(PersistentEntity entity, T criterion, StringBuilder q, List params);
    }


    private  final Map<Class, CriterionHandler> criterionHandlers = new HashMap() {
        {
            put(Like.class, new CriterionHandler<Like>() {
                public void handle(PersistentEntity entity, Like criterion, StringBuilder q, List params) {
                    String property = criterion.getProperty();
                    String pattern = criterion.getPattern();
                    validateProperty(entity,property,Like.class);
                q.append("jcr:like(@")
                .append(property)
                .append(",")
                .append("'")
                .append(pattern)
                .append("')");
                }
            });
            put(Between.class, new CriterionHandler<Between>() {
                public void handle(PersistentEntity entity, Between criterion, StringBuilder q, List params) {

                }
            });
            put(GreaterThanEquals.class, new CriterionHandler<GreaterThanEquals>() {
                public void handle(PersistentEntity entity, GreaterThanEquals criterion, StringBuilder q, List params) {
                    final String name = criterion.getProperty();
                    final Object value = criterion.getValue();
                    validateProperty(entity, name, GreaterThanEquals.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(GREATER_THAN_EQUALS);
                    q.append(value);
                }
            });
            put(GreaterThan.class, new CriterionHandler<GreaterThan>() {
                public void handle(PersistentEntity entity, GreaterThan criterion, StringBuilder q, List params) {
                    final String name = criterion.getProperty();
                    final Object value = criterion.getValue();
                    validateProperty(entity, name, GreaterThan.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(GREATER_THAN);
                    q.append(value);
                }
            });
            put(LessThanEquals.class, new CriterionHandler<LessThanEquals>() {
                public void handle(PersistentEntity entity, LessThanEquals criterion, StringBuilder q, List params) {
                    final String name = criterion.getProperty();
                    final Object value = criterion.getValue();
                    validateProperty(entity, name, LessThanEquals.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(LESS_THAN_EQUALS);
                    q.append(value);
                }
            });
            put(LessThan.class, new CriterionHandler<LessThan>() {
                public void handle(PersistentEntity entity, LessThan criterion, StringBuilder q, List params) {
                    final String name = criterion.getProperty();
                    final Object value = criterion.getValue();
                    validateProperty(entity, name, LessThan.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(LESS_THAN);
                    q.append(value);
                }
            });
            put(Equals.class, new CriterionHandler<Equals>() {
                public void handle(PersistentEntity entity, Equals eq, StringBuilder q, List params) {
                    final String name = eq.getProperty();
                    final Object value = eq.getValue();
                    validateProperty(entity, name, Equals.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(EQUALS);
                    if (value instanceof String || value  instanceof Boolean) {
                        q.append("'")
                                .append(value)
                                .append("'");
                    } else q.append(value);
                }
            });
            put(NotEquals.class, new CriterionHandler<NotEquals>() {
                public void handle(PersistentEntity entity, NotEquals nqe, StringBuilder q, List params) {
                    final String name = nqe.getProperty();
                    final Object value = nqe.getValue();
                    validateProperty(entity, name, Equals.class);
                    q.append(AT_SIGN)
                            .append(name)
                            .append(NOT_EQUALS);
                    if (value instanceof String || value  instanceof Boolean) {
                        q.append("'")
                                .append(value)
                                .append("'");
                    } else q.append(value);


                }
            });
            put(In.class, new CriterionHandler<In>() {
                public void handle(PersistentEntity entity, In criterion, StringBuilder q, List params) {
                    final String name = criterion.getName();
                    validateProperty(entity, name, In.class);
                    Disjunction dis = new Disjunction();
                    for (Object value : criterion.getValues()) {
                        dis.add(Restrictions.eq(name, value));
                    }
                    buildCondition(entity, dis, q, 0, params);
                }

            });
            put(Conjunction.class, new CriterionHandler<Junction>() {
                public void handle(PersistentEntity entity, Junction criterion, StringBuilder q, List params) {
                    System.out.println("conjection");
                    buildCondition(entity,  criterion, q, 0, params);
                }
            });
            put(Disjunction.class, new CriterionHandler<Junction>() {
                public void handle(PersistentEntity entity, Junction criterion, StringBuilder q, List params) {
                    System.out.println("disjunction");
                    buildCondition(entity, criterion, q, 0, params);

                }
            });
            /*  put(Negation.class, new CriterionHandler<Negation>() {
                public void handle(PersistentEntity entity, Negation criterion, StringBuilder q, List params) {
                    System.out.println("Negation");
                }
            });*/
        }
    };

      /**
     * Obtains the query string with variables embedded within the Query
     * @return The query string
     */
    public String getQueryString() {
        return getQueryString(null, false);
    }


    private static void validateProperty(PersistentEntity entity, String name, Class criterionType) {
        if (entity.getIdentity().getName().equals(name)) return;
        PersistentProperty prop = entity.getPropertyByName(name);
        if (prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use [" + criterionType.getSimpleName() + "] criterion on non-existent property: " + name);
        }
    }

    private List unsupportedProjection(String projectionType) {
        throw new InvalidDataAccessResourceUsageException("Cannot use [" + projectionType + "] projection. [" + projectionType + "] projections are not currently supported.");
    }

}

