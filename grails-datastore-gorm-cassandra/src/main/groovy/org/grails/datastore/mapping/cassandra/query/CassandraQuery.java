package org.grails.datastore.mapping.cassandra.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Query.Order.Direction;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;
import org.springframework.cassandra.core.ResultSetExtractor;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.querybuilder.Ordering;
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
    private ConversionService conversionService;
    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    private Map arguments = new HashMap();
    private boolean allowFiltering;
        
    public Query allowFiltering(boolean allowFiltering) {
        this.allowFiltering = allowFiltering;
        return this;
    }
    
    protected static interface QueryHandler<T> {
        public void handle(CassandraQuery cassandraQuery, T criterion, Where where);
    }

    static {
        queryHandlers.put(IdEquals.class, new QueryHandler<IdEquals>() {            
            public void handle(CassandraQuery cassandraQuery, IdEquals criterion, Where where) { 
                PersistentProperty property = cassandraQuery.entity.getIdentity();
                if (property != null) {
                    where.and(QueryBuilder.eq(property.getName(), criterion.getValue()));
                }
            }
        });
        
        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            public void handle(CassandraQuery cassandraQuery, Equals criterion, Where where) {
                where.and(QueryBuilder.eq(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), CassandraEntityPersister.convertPrimitiveToNative(criterion.getValue(), cassandraQuery.conversionService)));
            }
        });
        queryHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            public void handle(CassandraQuery cassandraQuery, GreaterThan criterion, Where where) {
                where.and(QueryBuilder.gt(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            public void handle(CassandraQuery cassandraQuery, GreaterThanEquals criterion, Where where) {
                where.and(QueryBuilder.gte(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            public void handle(CassandraQuery cassandraQuery, LessThan criterion, Where where) {
                where.and(QueryBuilder.lt(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            public void handle(CassandraQuery cassandraQuery, LessThanEquals criterion, Where where) {
                where.and(QueryBuilder.lte(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getValue()));
            }
        });
        queryHandlers.put(Between.class, new QueryHandler<Between>() {
            public void handle(CassandraQuery cassandraQuery, Between criterion, Where where) {
                where.and(QueryBuilder.gte(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getFrom()));
                where.and(QueryBuilder.lte(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), criterion.getTo()));
            }
        });
        queryHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(CassandraQuery cassandraQuery, In criterion, Where where) {
                List<Object> values = new ArrayList<Object>(criterion.getValues().size());                
                for (Object value : criterion.getValues()) {
                    value = CassandraEntityPersister.convertPrimitiveToNative(value, cassandraQuery.conversionService);
                    values.add(value);
                }               
                where.and(QueryBuilder.in(getPropertyName(cassandraQuery.cassandraPersistentEntity, criterion.getProperty()), values.toArray()));
            }
        });
    }

    public CassandraQuery(CassandraSession session, PersistentEntity entity) {
        super(session, entity);
        cassandraSession = session;
        cassandraTemplate = cassandraSession.getCassandraTemplate();
        cassandraPersistentEntity = cassandraTemplate.getCassandraMappingContext().getPersistentEntity(entity.getJavaClass());
        conversionService = cassandraTemplate.getConverter().getConversionService();
    }
    
    public void throwUnsupportedOperationException(String name) {
        throw new UnsupportedOperationException("Queries of type " + name + " are not supported by this implementation");
    }

    @Override
    public void setArguments(Map arguments) {
       this.arguments = arguments;        
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {

        List<Object> results = new LinkedList<Object>();
        if (criteria instanceof Disjunction) {
            throw new UnsupportedOperationException("Queries of type Disjunction (OR) are not supported by this implementation");
        } else if (criteria instanceof Conjunction) {
            final List<Projection> projectionList = projections().getProjectionList();            
            Select select = null;
            if (projectionList.isEmpty()) {
                select = QueryBuilder.select().all().from(getTableName(entity));
            } else {
                validateProjections(projectionList);
                select = buildQueryForProjections(entity, projectionList);
            }

            if (!criteria.getCriteria().isEmpty()) {
                buildCompositeClause(criteria, select.where());
            }
            
            List<Order> orderBys = getOrderBy();          
            if (!orderBys.isEmpty() ) {
                Ordering[] orderings = new Ordering[orderBys.size()];
                for (int i = 0; i < orderBys.size(); i++) {
                    Order orderBy = orderBys.get(i); 
                    if (orderBy.getDirection() == Direction.ASC) {
                        orderings[i] = QueryBuilder.asc(orderBy.getProperty());
                    } else {
                        orderings[i] = QueryBuilder.desc(orderBy.getProperty());
                    }
                }
                select.orderBy(orderings);
            } else {
                Order orderBy = ((Table) entity.getMapping().getMappedForm()).getOrderBy();
                if (orderBy != null) {
                    if (orderBy.getDirection() == Direction.ASC) {
                        select.orderBy(QueryBuilder.asc(orderBy.getProperty()));
                    } else {
                        select.orderBy(QueryBuilder.desc(orderBy.getProperty()));
                    }
                }
            }
            
            if (max > 0) { 
            	select.limit(max);
            }
            
            if (offset > 0) {
                throw new UnsupportedOperationException("Cassandra does not support offset with pagination");
            }
            
            if (arguments.containsKey(ARGUMENT_ALLOW_FILTERING) || allowFiltering) {
                select.allowFiltering();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Built Cassandra query to execute: " + select.toString());
            }
            if (projectionList.isEmpty()) {
                results = cassandraTemplate.select(select, entity.getJavaClass());
            } else {
                for (Projection projection : projectionList) {
                    if (projection instanceof CountProjection) {
                        long count = getCountResult(select);
                        results.add(count);
                    }
                    else if (projection instanceof IdProjection) {
                        PersistentProperty persistentProperty = entity.getIdentity();
                        if (persistentProperty != null) {
                            Class type = persistentProperty.getType();
                            results = cassandraTemplate.queryForList(select, type);
                        }
                    }
                    else if (projection instanceof PropertyProjection) {
                        PropertyProjection propertyProjection = (PropertyProjection) projection;
                        CassandraPersistentProperty persistentProperty = getPersistentProperty(cassandraPersistentEntity, propertyProjection.getPropertyName());
                        Class type = persistentProperty.getActualType();
                        results = cassandraTemplate.queryForList(select, type);
                    } else {
                        throwUnsupportedOperationException(projection.getClass().getSimpleName());
                    }
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
     */
    private void validateProjections(List<Projection> projections) {
        // of the grouping projects Cassandra only supports count(*) projection,
        // nothing else. Other kinds will have to be explicitly coded later...
        boolean hasCountProjection = false;
        for (Projection projection : projections) {
            if (!(PropertyProjection.class.equals(projection.getClass()) || 
                    CountProjection.class.equals(projection.getClass()) || 
                    IdProjection.class.equals(projection.getClass()))) {
                throwUnsupportedOperationException(projection.getClass().getSimpleName());
            }

            if (CountProjection.class.equals(projection.getClass())) {
                hasCountProjection = true;
            }
        }
        if (projections.size() > 1 && hasCountProjection) {
            throw new IllegalArgumentException("Can not mix count projection and other types of projections. You requested: " + projections);
        }        
    }

    private Select buildQueryForProjections(PersistentEntity entity, List<Projection> projectionList) {
        Select select = null;
        Selection selection = QueryBuilder.select();
        for (Projection projection : projectionList) {
            if (projection instanceof CountProjection) {
                selection.countAll();
            }
            else if (projection instanceof IdProjection) {
                PersistentProperty persistentProperty = entity.getIdentity();
                if (persistentProperty != null) {
                    selection.column(persistentProperty.getName());
                }            
            } else if (projection instanceof PropertyProjection) {
                String name = getPropertyName(cassandraPersistentEntity, ((PropertyProjection) projection).getPropertyName());
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
                    queryHandler.handle(this, criterion, where);
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

    private long getCountResult(Select select) {
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

    private static CassandraPersistentProperty getPersistentProperty(CassandraPersistentEntity<?> cassandraPersistentEntity, String propertyName) {
        CassandraPersistentProperty property = cassandraPersistentEntity.getPersistentProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Could not find property '" + propertyName + "' in entity '" + cassandraPersistentEntity.getName() + "'");
        }
        return property;
    }
    
    private static String getPropertyName(CassandraPersistentEntity<?> cassandraPersistentEntity, String propertyName) {
        CassandraPersistentProperty property = getPersistentProperty(cassandraPersistentEntity, propertyName);
        return property.getColumnName().toString();
    }
}
