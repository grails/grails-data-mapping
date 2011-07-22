package org.grails.datastore.mapping.query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.jcr.JcrSession;
import org.grails.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.projections.ManualProjections;
import org.springframework.extensions.jcr.JcrTemplate;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
@SuppressWarnings("hiding")
public class JcrQuery extends Query {
    private JcrEntityPersister entityPersister;
    private JcrTemplate jcrTemplate;
    private SimpleTypeConverter typeConverter;

    private ManualProjections manualProjections;

    public static final String ROOT_NODE = "//";
    public static final String GREATER_THAN_EQUALS = " >= ";
    public static final String LESS_THAN_EQUALS = " <= ";
    public static final String LOGICAL_AND = " and ";
    public static final String GREATER_THAN = " > ";
    public static final char SPACE = ' ';
    public static final char AT_SIGN = '@';
    public static final String LESS_THAN = " < ";
    public static final String EQUALS = " = ";
    public static final String NOT_EQUALS = " != ";
    public static final String ASCENDING = "ascending";
    public static final String DESCENDING = "descending";
    public static final String LOGICAL_OR = " or ";
    public static final String XS_DATE = "xs:date";

    public JcrQuery(JcrSession session, JcrTemplate jcrTemplate, PersistentEntity persistentEntity, JcrEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        this.jcrTemplate = jcrTemplate;
        this.manualProjections = new ManualProjections(entity);
        typeConverter = new SimpleTypeConverter();
    }

    protected JcrQuery(Session session, PersistentEntity entity) {
        super(session, entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        final ProjectionList projectionList = projections();
        List<String> uuids = new ArrayList<String>();
        List finalResults = null;
        if (criteria.isEmpty() && !(max != -1)) {
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
            }
            finalResults = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            if (projectionList.isEmpty()) {
                return finalResults;
            }

            List results = new ArrayList();
            for (Projection projection : projectionList.getProjectionList()) {
                 if (projection instanceof CountProjection) {
                    results.add(finalResults.size());
                } else if (projection instanceof MinProjection) {
                    MinProjection min = (MinProjection) projection;
                    results.add(manualProjections.min(finalResults, min.getPropertyName()));
                } else if (projection instanceof MaxProjection) {
                    MaxProjection max = (MaxProjection) projection;
                    results.add(manualProjections.max(finalResults, max.getPropertyName()));
                } else if (projection instanceof IdProjection) {
                    results.add(uuids);
                } else if (projection.getClass() == PropertyProjection.class) {
                    PropertyProjection propertyProjection = (PropertyProjection) projection;
                    final String propName = propertyProjection.getPropertyName();
                    PersistentProperty prop = entityPersister.getPersistentEntity().getPropertyByName(propName);
                    Class type = prop.getType();
                    List values = new ArrayList();
                    for (String uuid : uuids) {
                        Node node = jcrTemplate.getNodeByUUID(uuid);
                        try {
                            if (node.hasProperty(propName)) {
                                Property nodeProp = node.getProperty(propName);
                                values.add(nodeProp.getString());
                            }
                        } catch (RepositoryException e) {
                            throw new InvalidDataAccessResourceUsageException("Cannot execute PropertyProjection criterion on non-existent property: name[" + prop + "]");
                        }
                    }
                    final PersistentEntity associatedEntity = getSession().getMappingContext().getPersistentEntity(type.getName());
                    final boolean isEntityType = associatedEntity != null;
                    if (isEntityType) {
                        return getSession().retrieveAll(type, values);
                    }
                    for (Object value : values) {
                        results.add(typeConverter.convertIfNecessary(value, type));
                    }
                }
            }
            finalResults = results;
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
            if (uuids.isEmpty()) {
                return Collections.emptyList();
            }

            finalResults = getSession().retrieveAll(getEntity().getJavaClass(), uuids);
            IdProjection idProjection = null;
            if (!projectionList.isEmpty()) {
                List projectionResults = new ArrayList();
                for (Projection projection : projectionList.getProjectionList()) {
                    final String projectionType = projection.getClass().getSimpleName();
                    if (projection instanceof CountProjection) {
                        projectionResults.add(finalResults.size());
                    } else if (projection instanceof MaxProjection) {
                        MaxProjection min = (MaxProjection) projection;
                        finalResults.add(manualProjections.min(finalResults, min.getPropertyName()));
                    } else if (projection instanceof MinProjection) {
                        MinProjection min = (MinProjection) projection;
                        finalResults.add(manualProjections.min(finalResults, min.getPropertyName()));
                    } else {
                        if (projection instanceof SumProjection) {
                            return unsupportedProjection(projectionType);
                        }
                        if (projection instanceof AvgProjection) {
                            return unsupportedProjection(projectionType);
                            //} else if (projection instanceof PropertyProjection) {
                            //  PropertyProjection propertyProjection = (PropertyProjection) projection;
                            //   final String propName = propertyProjection.getPropertyName();
                            //   PersistentProperty prop = entityPersister.getPersistentEntity().getPropertyByName(propName);
                            //   return unsupportedProjection(projectionType);
                        }
                        if (projection instanceof IdProjection) {
                            idProjection = (IdProjection) projection;
                        }
                    }
                }
                if (!projectionResults.isEmpty()) {
                    return projectionResults;
                }
                if (idProjection != null) {
                    return uuids;
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

    @SuppressWarnings("unused")
    private List applyProjections(List results, ProjectionList projections) {
        List projectedResults = new ArrayList();
        for (Projection projection : projections.getProjectionList()) {
            if (projection instanceof CountProjection) {
                projectedResults.add(results.size());
            } else if (projection instanceof MinProjection) {
                MinProjection min = (MinProjection) projection;
                projectedResults.add(manualProjections.min(results, min.getPropertyName()));
            } else if (projection instanceof MaxProjection) {
                MaxProjection min = (MaxProjection) projection;
                projectedResults.add(manualProjections.max(results, min.getPropertyName()));
            }
        }
        if (projectedResults.isEmpty()) {
            return results;
        }
        return projectedResults;
    }

    protected String getQueryString(List params, @SuppressWarnings("unused") boolean distinct) {
        final StringBuilder q = new StringBuilder();
        q.append(ROOT_NODE);
        q.append(getEntity().getJavaClass().getSimpleName());

        if (!criteria.isEmpty()) {
            q.append("[");
            buildCondition(entity, criteria, q, 0, params);
            q.append("]");
        }

        validateQuery(q);

        for (Order order : orderBy) {
            String direction = null;
            if (order.getDirection().equals(Order.Direction.ASC)) {
                direction = ASCENDING;
            }
            else {
                direction = DESCENDING;
            }
            q.append(SPACE);
            q.append("order by @");
            q.append(order.getProperty());
            q.append(SPACE);
            q.append(direction);
        }
        return q.toString();
    }

    private StringBuilder validateQuery(StringBuilder q) {
        String tmp = q.toString();
        int length = tmp.length();
        Character c = tmp.charAt(length - 2);
        if (c.equals('[')) {
            tmp.subSequence(0, length - 2);
            q.delete(length - 2, length);
        }
        return q;
    }

    private static int buildCondition(PersistentEntity entity, Junction criteria, StringBuilder q, int index, List params) {
        final List<Criterion> criterionList = criteria.getCriteria();
        for (Iterator<Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Criterion criterion = iterator.next();
            final String operator = criteria instanceof Conjunction ? LOGICAL_AND : LOGICAL_OR;
            CriterionHandler qh = criterionHandlers.get(criterion.getClass());
            if (qh != null) {
                qh.handle(entity, criterion, q, params);
            }
            if (iterator.hasNext()) {
                q.append(operator);
            }
        }
        return index;
    }

    private static interface CriterionHandler<T> {
        void handle(PersistentEntity entity, T criterion, StringBuilder q, List params);
    }

    private static final Map<Class, CriterionHandler> criterionHandlers = new HashMap() {{
        put(Like.class, new CriterionHandler<Like>() {
            public void handle(PersistentEntity entity, Like criterion, StringBuilder q, List params) {
                String property = criterion.getProperty();
                String pattern = criterion.getPattern();
                validateProperty(entity, property, Like.class);
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
                final String name = criterion.getProperty();
                final Object value = criterion.getValue();
                validateProperty(entity, name, Between.class);
                q.append(AT_SIGN)
                        .append(name)
                        .append(GREATER_THAN_EQUALS)
                        .append(value);
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
                if (value instanceof Calendar || value instanceof Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    q.append(XS_DATE);
                    q.append("('");
                    q.append(sdf.format((Date)value));
                    q.append("')");
                }else q.append(value);
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
                if (value instanceof Calendar || value instanceof Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    q.append(XS_DATE);
                    q.append("('");
                    q.append(sdf.format((Date)value));
                    q.append("')");;
                }else q.append(value);
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
                if (value instanceof Calendar || value instanceof Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    q.append(XS_DATE);
                    q.append("('");
                    q.append(sdf.format((Date)value));
                    q.append("')");
                }else q.append(value);
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
                if (value instanceof Calendar || value instanceof Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    q.append(XS_DATE);
                    q.append("('");
                    q.append(sdf.format((Date)value));
                    q.append("')");
                }else q.append(value);
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
                if (value instanceof String || value instanceof Boolean) {
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
                if (value instanceof String || value instanceof Boolean) {
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
                buildCondition(entity, criterion, q, 0, params);
            }
        });
        put(Disjunction.class, new CriterionHandler<Junction>() {
            public void handle(PersistentEntity entity, Junction criterion, StringBuilder q, List params) {
                buildCondition(entity, criterion, q, 0, params);

            }
        });
        put(Negation.class, new CriterionHandler<Negation>() {
            public void handle(PersistentEntity entity, Negation criterion, StringBuilder q, List params) {
                List<Criterion> cris = criterion.getCriteria();
                Conjunction con = new Conjunction();
                for (Criterion c : cris) {
                    if (c instanceof Equals) {
                        con.add(Restrictions.ne(((Equals) c).getProperty(), ((Equals) c).getValue()));
                    }
                    if (c instanceof Conjunction) {
                        con.add(c);
                    }
                }
                buildCondition(entity, con, q, 0, params);
            }
        });
        }};

    /**
     * Obtains the query string with variables embedded within the Query
     *
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
