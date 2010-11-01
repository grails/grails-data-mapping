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
        List results = null;
        List<String> uuids = null;
        if (!criteria.isEmpty()) {
            System.out.println("!criteria.isEmpty");
            List criteriaList = criteria.getCriteria();
            uuids = executeSubQuery(criteria, criteriaList);
            results = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            System.out.println("results size with unempty criteria: " + results.size());
        }
        IdProjection idProjection = null;
        if (projectionList.isEmpty()) {
            System.out.println("projectionList.isEmpty");
            if (uuids == null)
                uuids = paginateResults(getEntity().getJavaClass().getSimpleName());
            //else {
            results = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            //}
            if (results != null)
                return results;
        } else {
            if (results == null) {
                uuids = paginateResults(getEntity().getJavaClass().getSimpleName());
                results = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            }
            System.out.println("projectionList is not Empty");
            List projectionResults = new ArrayList();
            for (Projection projection : projectionList.getProjectionList()) {
                final String projectionType = projection.getClass().getSimpleName();
                if (projection instanceof CountProjection) {
                    System.out.println("result size: " + results.size());
                    projectionResults.add(results.size());
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
            } else {
                results = paginateResults(getEntity().getJavaClass().getSimpleName());
            }
        }


        if (results != null) {
            if (idProjection != null) {
                return uuids;
            } else {
                return getSession().retrieveAll(getEntity().getJavaClass(), results);
            }
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> executeSubQuery(Junction junction, List<Criterion> criteriaList) {
        return getUUIDs(junction, entityPersister);
    }

    private List<String> paginateResults(String jcrNode) {
        StringBuffer query = new StringBuffer();
        query.append("//")
                .append(jcrNode);
        System.out.println(query.toString());
        QueryResult qr = jcrTemplate.query(query.toString(), javax.jcr.query.Query.XPATH);
        List<String> results = new ArrayList<String>();
        try {
            NodeIterator itr = qr.getNodes();
            while (itr.hasNext()) {
                Node node = itr.nextNode();
                results.add(node.getUUID());
            }
        } catch (RepositoryException e) {
            throw new InvalidDataAccessResourceUsageException("Cannot execute query. Entity [" + getEntity() + "] does not exist in the repository");
        }

        final int total = results.size();
        if (offset > total) return Collections.emptyList();
        // 0..3
        // 0..-1
        // 1..1
        int max = this.max; // 20
        int from = offset; // 10
        int to = max == -1 ? -1 : (offset + max) - 1;     // 15
        if (to >= total) to = -1;
        if (max != -1) {
            List<String> finalResult = results.subList(from, max);
            return finalResult;
        }
        return results;
    }

    private static interface CriterionHandler<T> {
        void handle(JcrEntityPersister entityPersister, List<String> uuids, T criterion);
    }

    private final Map<Class, CriterionHandler> criterionHandlers = new HashMap() {
        {
            put(Like.class, new CriterionHandler<Like>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Like criterion) {
                    String key = executeSubLike(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(Between.class, new CriterionHandler<Between>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Between criterion) {
                    String key = executeSubBetween(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(GreaterThanEquals.class, new CriterionHandler<GreaterThanEquals>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, GreaterThanEquals criterion) {
                    String key = executeGreaterThanEquals(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(GreaterThan.class, new CriterionHandler<GreaterThan>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, GreaterThan criterion) {
                    String key = executeGreaterThanEquals(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(LessThanEquals.class, new CriterionHandler<LessThanEquals>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, LessThanEquals criterion) {
                    String key = executeLessThanEquals(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(LessThan.class, new CriterionHandler<LessThan>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, LessThan criterion) {
                    String key = executeLessThanEquals(entityPersister, criterion);
                    uuids.add(key);
                }
            });
            put(Equals.class, new CriterionHandler<Equals>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Equals criterion) {
                    final String property = criterion.getProperty();
                    final Object value = criterion.getValue();
                    StringBuilder q = new StringBuilder();
                    q.append("//")
                            .append(getEntity().getJavaClass().getSimpleName())
                            .append("[@")
                            .append(property)
                            .append(" = ");
                    if (value instanceof String) {
                        q.append("'")
                                .append(value)
                                .append("'");
                    } else {
                        q.append(value);                          
                    }
                    q.append("]");

                    System.out.println(q.toString());
                    System.out.println("-----------------");
                    QueryResult qr = jcrTemplate.query(q.toString(), javax.jcr.query.Query.XPATH);
                    try {
                        NodeIterator itr = qr.getNodes();
                        while (itr.hasNext()) {
                            String uuid = itr.nextNode().getUUID();
                            uuids.add(uuid);
                            System.out.println(jcrTemplate.dump(jcrTemplate.getNodeByUUID(uuid)));
                        }
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
            });
            put(NotEquals.class, new CriterionHandler<NotEquals>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, NotEquals criterion) {
                    final String property = criterion.getProperty();
                    final Object value = criterion.getValue();
                    System.out.println("NotEquals");
                }
            });
            put(In.class, new CriterionHandler<In>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, In criterion) {
                    final String property = criterion.getName();
                    Disjunction dis = new Disjunction();
                    for (Object value : criterion.getValues()) {
                        dis.add(Restrictions.eq(property, value));
                    }
                    System.out.println("In");
                }

            });
            put(Conjunction.class, new CriterionHandler<Junction>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Junction criterion) {
                    System.out.println("Conjunction");
                }
            });
            put(Disjunction.class, new CriterionHandler<Junction>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Junction criterion) {
                    System.out.println("Disjunction");
                }
            });
            put(Negation.class, new CriterionHandler<Negation>() {
                public void handle(JcrEntityPersister entityPersister, List<String> uuids, Negation criterion) {
                    System.out.println("Negation");
                }
            });
        }

    };

    protected String executeLessThanEquals(JcrEntityPersister entityPersister, PropertyCriterion criterion) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    protected String executeGreaterThanEquals(JcrEntityPersister entityPersister, PropertyCriterion criterion) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    protected String executeSubBetween(JcrEntityPersister entityPersister, Between criterion) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    protected String executeSubLike(JcrEntityPersister entityPersister, Like criterion) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private List<String> getUUIDs(Junction criteria, JcrEntityPersister entityPersister) {
        List<Criterion> criteriaList = criteria.getCriteria();
        List<String> uuids = new ArrayList<String>();
        for (Criterion criterion : criteriaList) {
            CriterionHandler handler = criterionHandlers.get(criterion.getClass());
            if (handler != null) {
                handler.handle(entityPersister, uuids, criterion);
            }

        }
        System.out.println("uuids size: " + uuids.size());
        return uuids;
    }

    private List unsupportedProjection(String projectionType) {
        throw new InvalidDataAccessResourceUsageException("Cannot use [" + projectionType + "] projection. [" + projectionType + "] projections are not currently supported.");
    }

}

