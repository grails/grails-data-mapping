package org.grails.datastore.mapping.cassandra.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;
import org.springframework.cassandra.core.ResultSetExtractor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;

@SuppressWarnings("rawtypes")
public class CassandraQuery extends Query implements QueryArgumentsAware{

    private static final Log LOG = LogFactory.getLog(CassandraQuery.class);
    public static final String ARGUMENT_ALLOW_FILTERING = "allowFiltering";
    
    private CassandraSession cassandraSession;
    private CassandraTemplate cassandraTemplate;
    private CassandraPersistentEntity<?> cassandraPersistentEntity;
    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    private Map arguments = new HashMap();
    protected static interface QueryHandler<T> {
        public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, T criterion, Where where);
    }

    static {
        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, Equals criterion, Where where) {
                where.and(QueryBuilder.eq(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, GreaterThan criterion, Where where) {
                where.and(QueryBuilder.gt(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, GreaterThanEquals criterion, Where where) {
                where.and(QueryBuilder.gte(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, LessThan criterion, Where where) {
                where.and(QueryBuilder.lt(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, LessThanEquals criterion, Where where) {
                where.and(QueryBuilder.lte(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(Between.class, new QueryHandler<Between>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, Between criterion, Where where) {
                where.and(QueryBuilder.gte(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getFrom()));
                where.and(QueryBuilder.lte(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getTo()));
            }
        });
        queryHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(PersistentEntity entity, CassandraPersistentEntity<?> cassandraPersistentEntity, In criterion, Where where) {
                where.and(QueryBuilder.in(extractPropertyName(cassandraPersistentEntity, criterion.getProperty()), criterion.getValues().toArray()));
            }
        });
    }

    public CassandraQuery(CassandraSession session, PersistentEntity entity) {
        super(session, entity);
        cassandraSession = session;
        cassandraTemplate = cassandraSession.getCassandraTemplate();
        cassandraPersistentEntity = cassandraTemplate.getCassandraMappingContext().getPersistentEntity(entity.getJavaClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {

        List<Object> results = new LinkedList<Object>();
        if (criteria instanceof Disjunction) {
            throw new UnsupportedOperationException("Queries of type Disjunction (OR) are not supported by this implementation");
        } else if (criteria instanceof Conjunction) {
            final List<Projection> projectionList = projections().getProjectionList();
            boolean hasCountProjection = false;
            Select select = null;
            if (projectionList.isEmpty()) {
                select = QueryBuilder.select().all().from(getTableName(entity));
            } else {
                hasCountProjection = validateProjectionsAndCheckIfCountIsPresent(projectionList);
                select = buildQueryForProjections(entity, projectionList);
            }

            if (!criteria.getCriteria().isEmpty()) {
                buildCompositeClause(criteria, select.where());
            }
            if (arguments.containsKey(ARGUMENT_ALLOW_FILTERING)) {
                select.allowFiltering();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Built Cassandra query to execute: " + select.toString());
            }
            if (projectionList.isEmpty()) {
                results = cassandraTemplate.select(select, entity.getJavaClass());
            } else {
                if (hasCountProjection) {
                    long count = executeForCount(select);
                    results.add(count);
                }
            }
        } else {
            throwUnsupportedOperationException(criteria.getClass().getSimpleName());
        }
        return results;
    }

    private String getTableName(PersistentEntity entity) {
        return cassandraTemplate.getTableName(entity.getJavaClass()).toString();
    }

    /**
     * Ensure that only property, or count projections are provided, and that
     * the combination of them is meaningful. Throws exception if something is
     * invalid.
     * 
     * @param projections
     * @returns true if count projection is present, false otherwise.
     */
    private boolean validateProjectionsAndCheckIfCountIsPresent(List<Projection> projections) {
        // of the grouping projects Cassandra only supports count(*) projection,
        // nothing else. Other kinds will have to be explicitly coded later...
        boolean hasCountProjection = false;
        for (Projection projection : projections) {
            if (!(PropertyProjection.class.equals(projection.getClass()) || CountProjection.class.equals(projection.getClass()))) {
                throwUnsupportedOperationException(projection.getClass().getSimpleName());
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

    private Select buildQueryForProjections(PersistentEntity entity, List<Projection> projectionList) {
        Select select = null;
        Selection selection = QueryBuilder.select();
        for (Projection projection : projectionList) {
            if (CountProjection.class.equals(projection.getClass())) {
                selection.countAll();
            } else if (PropertyProjection.class.equals(projection.getClass())) {
                String name = extractPropertyName(cassandraPersistentEntity, ((PropertyProjection) projection).getPropertyName());
                selection.column(name);
            }
        }
        select = selection.from(getTableName(entity));
        return select;
    }

    @SuppressWarnings("unchecked")
    private void buildCompositeClause(Junction criteria, Where where) {
        for (Criterion criterion : criteria.getCriteria()) {
            if (criterion instanceof PropertyNameCriterion) {
                QueryHandler queryHandler = queryHandlers.get(criterion.getClass());
                if (queryHandler != null) {
                    queryHandler.handle(entity, cassandraPersistentEntity, criterion, where);
                } else {
                    throwUnsupportedOperationException(criterion.getClass().getSimpleName());
                }
            
            }
            else if (criterion instanceof Conjunction) {
                buildCompositeClause((Junction) criterion, where);
            }
            else {
                throwUnsupportedOperationException(criterion.getClass().getSimpleName());
            }
        }
    }

    private long executeForCount(Select select) {
        return cassandraTemplate.query(select, new ResultSetExtractor<Long>() {
            @Override
            public Long extractData(ResultSet rs) throws DriverException, DataAccessException {

                Row row = rs.one();
                if (row == null) {
                    throw new InvalidDataAccessApiUsageException(String.format("count query did not return any results"));
                }

                return row.getLong(0);
            }
        });
    }

    private String getKeyspace(ClassMapping<Family> cm, String defaultValue) {
        String keyspace = null;
        if (cm.getMappedForm() != null) {
            keyspace = cm.getMappedForm().getKeyspace();
        }
        if (keyspace == null) {
            keyspace = defaultValue;
        }
        return keyspace;
    }

    private static String extractPropertyName(CassandraPersistentEntity<?> cassandraPersistentEntity, String propertyName) {
        CassandraPersistentProperty property = cassandraPersistentEntity.getPersistentProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Could not find property '" + propertyName + "' in entity '" + cassandraPersistentEntity.getName() + "'");
        }
        return property.getColumnName().toString();
    }

    public void throwUnsupportedOperationException(String name) {
        throw new UnsupportedOperationException("Queries of type " + name + " are not supported by this implementation");
    }

    @Override
    public void setArguments(Map arguments) {
       this.arguments = arguments;        
    }
}
