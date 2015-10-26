package org.grails.datastore.gorm.neo4j;

import groovy.lang.GroovyObject;
import org.grails.datastore.gorm.neo4j.engine.*;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.impl.*;
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Custom;
import org.grails.datastore.mapping.model.types.Simple;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.transactions.SessionHolder;
import org.grails.datastore.mapping.transactions.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.*;

/**
 *
 * Represents a session for interacting with Neo4j
 *
 * Created by stefan on 03.03.14.
 *
 * @author Stefan
 * @author Graeme Rocher
 *
 * @since 1.0
 *
 */
public class Neo4jSession extends AbstractSession<GraphDatabaseService> {

    public static final String CYPHER_DYNAMIC_RELATIONSHIP_MERGE = "MATCH (a%s {__id__:{id}}), (b%s {__id__:{related}}) MERGE (a)-[:%s]->(b)";
    private static Logger log = LoggerFactory.getLogger(Neo4jSession.class);


    /** map node id -> hashmap of relationship types showing startNode id and endNode id */
    protected final GraphDatabaseService graphDatabaseService;


    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, GraphDatabaseService graphDatabaseService) {
        super(datastore, mappingContext, publisher, stateless);
        if(log.isDebugEnabled()) {
            log.debug("session created");
        }
        this.graphDatabaseService = graphDatabaseService;
    }


    /**
     * Gets a Neo4jEntityPersister for the given object
     *
     * @param o The object
     * @return A Neo4jEntityPersister
     */
    public Neo4jEntityPersister getEntityPersister(Object o) {
        return (Neo4jEntityPersister)getPersister(o);
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null ) ? new Neo4jEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        throw new IllegalStateException("Use beingTransaction(TransactionDefinition) instead");
    }

    public Transaction beginTransaction(TransactionDefinition transactionDefinition) {
        if (transaction != null) {
            return transaction;
        }
        else {
            // if there is a current transaction, return that, since Neo4j doesn't really supported transaction nesting
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getDatastore());
                transaction = sessionHolder.getTransaction();
            } else {

                transaction = new Neo4jTransaction(graphDatabaseService, transactionDefinition);
            }
            return transaction;
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if(transaction != null) {
            Neo4jTransaction transaction = (Neo4jTransaction) getTransaction();
            transaction.getTransaction().close();
            this.transaction = null;
        }
    }

    @Override
    public Neo4jDatastore getDatastore() {
        return (Neo4jDatastore) super.getDatastore();
    }

    @Override
    public GraphDatabaseService getNativeInterface() {
        return graphDatabaseService;
    }

    @Override
    protected void flushPendingUpdates(Map<PersistentEntity, Collection<PendingUpdate>> updates) {

        // UPDATE statements cannot be batched, but optimise statements for dirty checking
        final Set<PersistentEntity> entities = updates.keySet();
        final Neo4jMappingContext mappingContext = (Neo4jMappingContext) getMappingContext();


        for (PersistentEntity entity : entities) {
            final Collection<PendingUpdate> pendingUpdates = updates.get(entity);
            final boolean isVersioned = entity.hasProperty(GormProperties.VERSION, Long.class) && entity.isVersioned();

            for (PendingUpdate pendingUpdate : pendingUpdates) {
                List<PendingOperation> preOperations = pendingUpdate.getPreOperations();
                executePendings(preOperations);

                pendingUpdate.run();

                if(pendingUpdate.isVetoed()) continue;

                final EntityAccess access = pendingUpdate.getEntityAccess();

                final String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance(access.getEntity());
                final StringBuilder cypherStringBuilder = new StringBuilder();

                Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
                final Object id = pendingUpdate.getNativeKey();
                params.put(GormProperties.IDENTITY, id);
                Map<String, Object> simpleProps = new HashMap<String, Object>();

                cypherStringBuilder.append(CypherBuilder.CYPHER_MATCH_ID);

                final Object object = pendingUpdate.getObject();
                final DirtyCheckable dirtyCheckable = (DirtyCheckable) object;
                final List<String> dirtyPropertyNames = dirtyCheckable.listDirtyPropertyNames();
                final List<String> nulls = new ArrayList<String>();
                for (String dirtyPropertyName : dirtyPropertyNames) {
                    final PersistentProperty property = entity.getPropertyByName(dirtyPropertyName);
                    if(property !=null){
                        if (property instanceof Simple) {
                            String name = property.getName();
                            Object value = access.getProperty(name);
                            if (value != null) {
                                simpleProps.put(name,  mappingContext.convertToNative(value));
                            }
                            else {
                                nulls.add(name);
                            }
                        }
                        else if(property instanceof Custom) {
                            Custom<Map<String,Object>> custom = (Custom<Map<String,Object>>)property;
                            final CustomTypeMarshaller<Object, Map<String, Object>, Map<String, Object>> customTypeMarshaller = custom.getCustomTypeMarshaller();
                            Object value = access.getProperty(property.getName());
                            customTypeMarshaller.write(custom, value, simpleProps);
                        }
                    }
                }

                amendMapWithUndeclaredProperties(simpleProps, object, mappingContext, nulls);
                final boolean hasPropertyUpdates = simpleProps.isEmpty();
                if(hasPropertyUpdates && nulls.isEmpty()) {
                    // if there are no simply property updates then only the assocations were dirty
                    // reset track changes
                    dirtyCheckable.trackChanges();
                    executePendings(pendingUpdate.getCascadeOperations());

                }
                else {
                    params.put(CypherBuilder.PROPS, simpleProps);
                    if(isVersioned) {
                        Long version = (Long) access.getProperty(GormProperties.VERSION);
                        if (version == null) {
                            version = 0l;
                        }
                        params.put(GormProperties.VERSION, version);
                        cypherStringBuilder.append(" AND n.version={version}");
                        long newVersion = version + 1;
                        simpleProps.put(GormProperties.VERSION, newVersion);
                        access.setProperty(GormProperties.VERSION, newVersion);
                    }
                    cypherStringBuilder.append(" SET n+={props}");
                    if(!nulls.isEmpty()) {
                        for (String aNull : nulls) {
                            cypherStringBuilder.append(",n.").append(aNull).append(" = NULL");
                        }
                    }
                    cypherStringBuilder.append(" RETURN id(n) as id");
                    String cypher = String.format(cypherStringBuilder.toString(), labels);
                    if( log.isDebugEnabled() ) {
                        log.debug("UPDATE Cypher [{}] for parameters [{}]", cypher, params);
                    }

                    final Result executionResult = graphDatabaseService.execute(cypher, params);
                    Map<String, Object> result = IteratorUtil.singleOrNull(executionResult);
                    if (result == null && isVersioned) {
                        throw new OptimisticLockingException(entity, id);
                    }
                    else {
                        // reset track changes
                        dirtyCheckable.trackChanges();
                        executePendings(pendingUpdate.getCascadeOperations());
                    }
                }
            }
        }

    }

    @Override
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {

        // batch up all inserts into a single CREATE statement
        final Set<PersistentEntity> entities = inserts.keySet();
        final Neo4jMappingContext mappingContext = (Neo4jMappingContext) getMappingContext();
        int i = 0;
        boolean first = true;
        boolean hasInserts = false;
        StringBuilder createCypher = new StringBuilder(CypherBuilder.CYPHER_CREATE);
        final Map<String, Object> params = new HashMap<String, Object>(inserts.size());
        Collection<PendingOperation> cascadingOperations = new ArrayList<PendingOperation>();
        for (PersistentEntity entity : entities) {
            final Collection<PendingInsert> entityInserts = inserts.get(entity);
            for (final PendingInsert entityInsert : entityInserts) {
                List<PendingOperation> preOperations = entityInsert.getPreOperations();
                for (PendingOperation preOperation : preOperations) {
                    preOperation.run();
                }

                entityInsert.run();

                if(entityInsert.isVetoed()) continue;

                cascadingOperations.addAll(entityInsert.getCascadeOperations());

                hasInserts = true;
                final List<PersistentProperty> persistentProperties = entity.getPersistentProperties();
                Map<String, Object> simpleProps = new HashMap<String, Object>(persistentProperties.size());
                simpleProps.put(CypherBuilder.IDENTIFIER, entityInsert.getNativeKey());


                i++;
                if(!first) {
                    createCypher.append(',');
                    createCypher.append('\n');
                }
                else {
                    first = false;
                }
                final Object obj = entityInsert.getObject();
                final GraphPersistentEntity graphEntity = (GraphPersistentEntity) entity;
                final String labels = graphEntity.getLabelsWithInheritance(obj);

                String cypher = String.format("(n"+i+"%s {props"+i+"})", labels);
                createCypher.append(cypher);
                params.put("props" + i, simpleProps);

                Map<String, List<Object>> dynamicRelProps = amendMapWithUndeclaredProperties(simpleProps, obj, mappingContext);
                final EntityAccess access = entityInsert.getEntityAccess();
                // build a properties map for each CREATE statement
                for (PersistentProperty pp : persistentProperties) {
                    if (pp instanceof Simple) {
                        String name = pp.getName();
                        Object value = access.getProperty(name);
                        if (value != null) {
                            simpleProps.put(name, mappingContext.convertToNative(value));
                        }
                    }
                    else if(pp instanceof Custom) {
                        Custom<Map<String,Object>> custom = (Custom<Map<String,Object>>)pp;
                        final CustomTypeMarshaller<Object, Map<String, Object>, Map<String, Object>> customTypeMarshaller = custom.getCustomTypeMarshaller();
                        Object value = access.getProperty(pp.getName());
                        customTypeMarshaller.write(custom, value, simpleProps);
                    }
                }

                if(graphEntity.hasDynamicAssociations()) {
                    for (final Map.Entry<String, List<Object>> e: dynamicRelProps.entrySet()) {
                        for (final Object o : e.getValue()) {

                            final GraphPersistentEntity gpe = (GraphPersistentEntity) mappingContext.getPersistentEntity(o.getClass().getName());
                            if(gpe != null) {
                                final EntityAccess dynamicAccess = gpe.getMappingContext().createEntityAccess(gpe, o);

                                cascadingOperations.add(new PendingOperationAdapter<Object, Long>(gpe, (Long) dynamicAccess.getIdentifier(), o) {
                                    @Override
                                    public void run() {
                                        final String labelsWithInheritance = gpe.getLabelsWithInheritance(o);
                                        String cypher = String.format(CYPHER_DYNAMIC_RELATIONSHIP_MERGE, labels, labelsWithInheritance, e.getKey());
                                        Map<String,Object> p =  new LinkedHashMap<String, Object>(2);
                                        p.put(GormProperties.IDENTITY, entityInsert.getNativeKey());
                                        p.put(CypherBuilder.RELATED, dynamicAccess.getIdentifier());

                                        if(log.isDebugEnabled()) {
                                            log.debug("MERGE Cypher [{}] for parameters [{}]", cypher, p);
                                        }
                                        graphDatabaseService.execute(cypher, p);
                                    }
                                });
                            }

                        }
                    }
                }
            }

        }


        if(hasInserts) {

            final String finalCypher = createCypher.toString();
            if(log.isDebugEnabled()) {
                log.debug("CREATE Cypher [{}] for parameters [{}]", finalCypher, params);
            }
            graphDatabaseService.execute(finalCypher, params);

            executePendings(cascadingOperations);
        }

    }

    protected Map<String, List<Object>> amendMapWithUndeclaredProperties(Map<String, Object> simpleProps, Object pojo, MappingContext mappingContext) {
        return amendMapWithUndeclaredProperties(simpleProps, pojo, mappingContext, new ArrayList<String>());
    }

    protected Map<String, List<Object>> amendMapWithUndeclaredProperties(Map<String, Object> simpleProps, Object pojo, MappingContext mappingContext, List<String> nulls) {
        Map<String, List<Object>> dynRelProps = new LinkedHashMap<String, List<Object>>();
        Map<String,Object> map = (Map<String, Object>) getAttribute(pojo, Neo4jGormEnhancer.UNDECLARED_PROPERTIES);
        if (map!=null) {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if(value == null) {
                    nulls.add(key);
                    continue;
                }

                if  (mappingContext.isPersistentEntity(value)) {
                    List<Object> objects = getOrInit(dynRelProps, key);
                    objects.add(value);
                } else if (isCollectionWithPersistentEntities(value, mappingContext)) {
                    List<Object> objects = getOrInit(dynRelProps, key);
                    objects.addAll((Collection) value);
                } else {
                    simpleProps.put(key, ((Neo4jMappingContext)mappingContext).convertToNative(value));
                }
            }
        }
        return dynRelProps;
    }

    private List<Object> getOrInit(Map<String, List<Object>> dynRelProps, String key) {
        List<Object> objects = dynRelProps.get(key);
        if(objects == null) {
            objects = new ArrayList<Object>();
            dynRelProps.put(key, objects);
        }
        return objects;
    }


    public static boolean isCollectionWithPersistentEntities(Object o, MappingContext mappingContext) {
        if (!(o instanceof Collection)) {
            return false;
        }
        else {
            Collection c = (Collection)o;
            for (Object obj : c) {
                if( mappingContext.isPersistentEntity(obj) ) return true;
            }
        }
        return false;
    }


    @Override
    public void flush() {
        persistDirtyButUnsavedInstances();
        super.flush();
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        super.postFlush(hasUpdates);
        if (publisher!=null) {
            publisher.publishEvent(new SessionFlushedEvent(this));
        }
    }

    /**
     * in case a
     * known instance is modified and not explicitly saved, we track dirty state here and spool them for persisting
     */
    private void persistDirtyButUnsavedInstances() {
        for (Map<Serializable, Object> cache : firstLevelCache.values()) {
            for (Object obj: cache.values()) {
                if (obj instanceof DirtyCheckable) {
                    boolean isDirty = ((DirtyCheckable)obj).hasChanged();
                    if (isDirty) {
                        persist(obj);
                    }
                }
            }
        }
    }


    // TODO: Optimize batch deletes!
    @Override
    public long deleteAll(QueryableCriteria criteria) {
        return super.deleteAll(criteria);
    }

    // TODO: Optimize batch updates!
    @Override
    public long updateAll(QueryableCriteria criteria, Map<String, Object> properties) {
        return super.updateAll(criteria, properties);
    }
}


