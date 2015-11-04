package org.grails.datastore.gorm.neo4j;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
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
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.Custom;
import org.grails.datastore.mapping.model.types.Simple;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Restrictions;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.transactions.SessionHolder;
import org.grails.datastore.mapping.transactions.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    public static final String CYPHER_DYNAMIC_RELATIONSHIP_MERGE = "MATCH (a%s {"+CypherBuilder.IDENTIFIER+":{id}}), (b%s {"+CypherBuilder.IDENTIFIER+":{related}}) MERGE (a)-[:%s]->(b)";
    public static final String CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NATIVE_TO_NATIVE = "MATCH  (a%s), (b%s) WHERE ID(a) = {id} AND ID(b) = {related} MERGE (a)-[:%s]->(b)";
    public static final String CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NATIVE_TO_NON_NATIVE = "MATCH  (a%s), (b%s) WHERE ID(a) = {id} AND b."+CypherBuilder.IDENTIFIER+" = {related} MERGE (a)-[:%s]->(b)";
    public static final String CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NON_NATIVE_TO_NATIVE = "MATCH  (a%s), (b%s) WHERE a."+CypherBuilder.IDENTIFIER+" = {id} AND ID(b) = {related} MERGE (a)-[:%s]->(b)";

    private static final String COUNT_RETURN = "count(n) as total";
    private static final String TOTAL_COUNT = "total";
    private static Logger log = LoggerFactory.getLogger(Neo4jSession.class);
    private static final EvictionListener<RelationshipUpdateKey, Collection<Serializable>> EXCEPTION_THROWING_INSERT_LISTENER =
            new EvictionListener<RelationshipUpdateKey, Collection<Serializable>>() {
                public void onEviction(RelationshipUpdateKey association, Collection<Serializable> value) {
                    throw new DataAccessResourceFailureException("Maximum number (5000) of relationship update operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
                }
            };

    protected Map<RelationshipUpdateKey, Collection<Serializable>> pendingRelationshipInserts =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Serializable>>()
                    .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                    .maximumWeightedCapacity(5000).build();

    protected Map<RelationshipUpdateKey, Collection<Serializable>> pendingRelationshipDeletes =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Serializable>>()
                    .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                    .maximumWeightedCapacity(5000).build();


    /** map node id -> hashmap of relationship types showing startNode id and endNode id */
    protected final GraphDatabaseService graphDatabaseService;


    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, GraphDatabaseService graphDatabaseService) {
        super(datastore, mappingContext, publisher, stateless);
        if(log.isDebugEnabled()) {
            log.debug("Session created");
        }
        this.graphDatabaseService = graphDatabaseService;
    }




    /**
     * Adds a relationship that is pending insertion
     *
     * @param association The association
     * @param id The id
     */
    public void addPendingRelationshipInsert(Serializable parentId, Association association, Serializable id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipInserts);
    }

    /**
     * Adds a relationship that is pending deletion
     *
     * @param association The association
     * @param id The id
     */
    public void addPendingRelationshipDelete(Serializable parentId, Association association, Serializable id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipDeletes);
    }

    protected void addRelationshipUpdate(Serializable parentId, Association association, Serializable id, Map<RelationshipUpdateKey, Collection<Serializable>> targetMap) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(parentId, association);
        Collection<Serializable> inserts = targetMap.get(key);
        if (inserts == null) {
            inserts = new ConcurrentLinkedQueue<Serializable>();
            targetMap.put(key, inserts);
        }

        inserts.add(id);
    }

    @Override
    protected void clearPendingOperations() {
        try {
            super.clearPendingOperations();
        } finally {
            pendingRelationshipInserts.clear();
        }
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
        return beginTransactionInternal(transactionDefinition, false);
    }

    protected Transaction beginTransactionInternal(TransactionDefinition transactionDefinition, boolean sessionCreated) {
        if (transaction != null && transaction.isActive()) {
            return transaction;
        }
        else {
            // if there is a current transaction, return that, since Neo4j doesn't really supported transaction nesting
            Transaction tx = null;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getDatastore());
                tx = sessionHolder.getTransaction();
            }

            if(tx == null || !tx.isActive()) {
                if(transactionDefinition.getName() == null) {
                    transactionDefinition = createDefaultTransactionDefinition(transactionDefinition);
                }
                tx = new Neo4jTransaction(graphDatabaseService, transactionDefinition, sessionCreated);
            }
            this.transaction = tx;
            return transaction;
        }
    }

    @Override
    public void disconnect() {
        if(isConnected()) {

            super.disconnect();
            // if there is no active synchronization defined by a TransactionManager then close the transaction is if was created
            try {
                if(transaction != null && !isSynchronizedWithTransaction) {
                    Neo4jTransaction transaction = (Neo4jTransaction) getTransaction();

                    transaction.close();
                }
            } catch (IOException e) {
                log.error("Error closing transaction: " + e.getMessage(), e);
            }
            finally {
                if(log.isDebugEnabled()) {
                    log.debug("Session closed");
                }
                this.transaction = null;
            }
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
            GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity) entity;
            final boolean isNativeId = graphPersistentEntity.getIdGenerator() == null;
            final boolean isVersioned = entity.hasProperty(GormProperties.VERSION, Long.class) && entity.isVersioned();

            for (PendingUpdate pendingUpdate : pendingUpdates) {
                final List<PendingOperation> preOperations = pendingUpdate.getPreOperations();
                executePendings(preOperations);

                pendingUpdate.run();

                if(pendingUpdate.isVetoed()) continue;

                final EntityAccess access = pendingUpdate.getEntityAccess();
                final List<PendingOperation<Object, Serializable>> cascadingOperations = new ArrayList<PendingOperation<Object, Serializable>>(pendingUpdate.getCascadeOperations());

                final String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance(access.getEntity());
                final StringBuilder cypherStringBuilder = new StringBuilder();

                final Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
                final Serializable id = (Serializable)pendingUpdate.getNativeKey();
                params.put(GormProperties.IDENTITY, id);
                final Map<String, Object> simpleProps = new HashMap<String, Object>();

                if(isNativeId) {
                    cypherStringBuilder.append(CypherBuilder.CYPHER_MATCH_NATIVE_ID);
                }
                else {
                    cypherStringBuilder.append(CypherBuilder.CYPHER_MATCH_ID);
                }

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

                for (Association association : entity.getAssociations()) {
                    processPendingRelationshipUpdates(access, id, association, cascadingOperations);
                }
                amendMapWithUndeclaredProperties(simpleProps, object, mappingContext, nulls);
                final boolean hasNoUpdates = simpleProps.isEmpty();
                if(hasNoUpdates && nulls.isEmpty()) {
                    // if there are no simply property updates then only the assocations were dirty
                    // reset track changes
                    dirtyCheckable.trackChanges();
                    executePendings(cascadingOperations);

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
                    cypherStringBuilder.append(Neo4jEntityPersister.RETURN_NODE_ID);
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
                        executePendings(cascadingOperations);
                    }
                }
            }
        }

    }

    private void processPendingRelationshipUpdates(EntityAccess parent, Serializable parentId, Association association, List<PendingOperation<Object, Serializable>> cascadingOperations) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(parentId, association);
        final Collection<Serializable> pendingInserts = pendingRelationshipInserts.get(key);
        if(pendingInserts != null) {
            cascadingOperations.add(new RelationshipPendingInsert(parent, association, pendingInserts, graphDatabaseService, true));
        }
        final Collection<Serializable> pendingDeletes = pendingRelationshipDeletes.get(key);
        if(pendingDeletes != null) {
            cascadingOperations.add(new RelationshipPendingDelete(parent, association, pendingDeletes, graphDatabaseService));
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
        List<PendingOperation<Object, Serializable>> cascadingOperations = new ArrayList<PendingOperation<Object, Serializable>>();
        for (PersistentEntity entity : entities) {
            final Collection<PendingInsert> entityInserts = inserts.get(entity);
            for (final PendingInsert entityInsert : entityInserts) {

                if(entityInsert.wasExecuted()) {


                    for (Association association : entity.getAssociations()) {
                        final EntityAccess entityAccess = entityInsert.getEntityAccess();
                        processPendingRelationshipUpdates(entityAccess, (Serializable) entityAccess.getIdentifier(), association, cascadingOperations);
                    }
                    cascadingOperations.addAll(entityInsert.getCascadeOperations());
                }
                else {

                    List<PendingOperation> preOperations = entityInsert.getPreOperations();
                    for (PendingOperation preOperation : preOperations) {
                        preOperation.run();
                    }

                    entityInsert.run();

                    if(entityInsert.isVetoed()) continue;

                    cascadingOperations.addAll(entityInsert.getCascadeOperations());

                    hasInserts = true;
                    i++;
                    if(!first) {
                        createCypher.append(',');
                        createCypher.append('\n');
                    }
                    else {
                        first = false;
                    }

                    buildEntityCreateOperation(createCypher, String.valueOf(i), entity, entityInsert, params, cascadingOperations, mappingContext);
                }
            }

        }


        if(hasInserts) {

            final String finalCypher = createCypher.toString();
            if(log.isDebugEnabled()) {
                log.debug("CREATE Cypher [{}] for parameters [{}]", finalCypher, params);
            }
            graphDatabaseService.execute(finalCypher, params);

        }
        executePendings(cascadingOperations);

    }

    public String buildEntityCreateOperation(PersistentEntity entity, PendingInsert entityInsert, Map<String, Object> params, List<PendingOperation<Object, Serializable>> cascadingOperations) {
        StringBuilder createCypher = new StringBuilder(CypherBuilder.CYPHER_CREATE);
        buildEntityCreateOperation(createCypher, "", entity, entityInsert, params, cascadingOperations, (Neo4jMappingContext) getMappingContext());
        return createCypher.toString();
    }

    public void buildEntityCreateOperation(StringBuilder createCypher, String index, PersistentEntity entity, PendingInsert entityInsert, Map<String, Object> params, List<PendingOperation<Object, Serializable>> cascadingOperations, Neo4jMappingContext mappingContext) {
        GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity) entity;
        final List<PersistentProperty> persistentProperties = entity.getPersistentProperties();
        Map<String, Object> simpleProps = new HashMap<String, Object>(persistentProperties.size());
        final Serializable id = (Serializable)entityInsert.getNativeKey();
        if(graphPersistentEntity.getIdGenerator() != null) {
            simpleProps.put(CypherBuilder.IDENTIFIER, id);
        }


        final Object obj = entityInsert.getObject();
        final GraphPersistentEntity graphEntity = (GraphPersistentEntity) entity;
        final String labels = graphEntity.getLabelsWithInheritance(obj);

        String cypher = String.format("(n"+ index +"%s {props"+ index +"})", labels);
        createCypher.append(cypher);
        params.put("props" + index, simpleProps);

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
            else if((pp instanceof Association) && id != null) {
                Association association = (Association) pp;
                processPendingRelationshipUpdates(access, id, association, cascadingOperations);
            }
        }

        if(graphEntity.hasDynamicAssociations()) {
            for (final Map.Entry<String, List<Object>> e: dynamicRelProps.entrySet()) {
                for (final Object o : e.getValue()) {

                    final GraphPersistentEntity associated = (GraphPersistentEntity) mappingContext.getPersistentEntity(o.getClass().getName());
                    if(associated != null) {
                        final EntityAccess dynamicAccess = associated.getMappingContext().createEntityAccess(associated, o);

                        cascadingOperations.add(new PendingOperationAdapter<Object, Serializable>(associated, (Serializable) dynamicAccess.getIdentifier(), o) {
                            @Override
                            public void run() {
                                final String labelsWithInheritance = associated.getLabelsWithInheritance(o);
                                String cypher;
                                final boolean isNative = associated.getIdGenerator() == null;
                                final boolean isAssociatedNative = graphEntity.getIdGenerator() == null;
                                if(isNative && isAssociatedNative) {
                                    cypher = String.format(CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NATIVE_TO_NATIVE, labels, labelsWithInheritance, e.getKey());
                                }
                                else if(!isNative && isAssociatedNative ) {
                                    cypher = String.format(CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NON_NATIVE_TO_NATIVE, labels, labelsWithInheritance, e.getKey());
                                }
                                else if(isNative && !isAssociatedNative) {
                                    cypher = String.format(CYPHER_DYNAMIC_RELATIONSHIP_MERGE_NATIVE_TO_NON_NATIVE, labels, labelsWithInheritance, e.getKey());
                                }
                                else {
                                    cypher = String.format(CYPHER_DYNAMIC_RELATIONSHIP_MERGE, labels, labelsWithInheritance, e.getKey());
                                }
                                Map<String,Object> p =  new LinkedHashMap<String, Object>(2);
                                p.put(GormProperties.IDENTITY, id);
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


    @Override
    protected void flushPendingDeletes(Map<PersistentEntity, Collection<PendingDelete>> pendingDeletes) {

        final Set<PersistentEntity> persistentEntities = pendingDeletes.keySet();
        for (PersistentEntity entity : persistentEntities) {

            final Collection<PendingDelete> deletes = pendingDeletes.get(entity);
            final Collection<Object> ids = new ArrayList<Object>();
            Collection<PendingOperation> cascadingOperations = new ArrayList<PendingOperation>();

            for (PendingDelete delete : deletes) {

                List<PendingOperation> preOperations = delete.getPreOperations();
                for (PendingOperation preOperation : preOperations) {
                    preOperation.run();
                }

                delete.run();

                if(delete.isVetoed()) continue;

                final Object id = delete.getNativeKey();
                ids.add(id);
                cascadingOperations.addAll(delete.getCascadeOperations());
            }

            final Neo4jQuery deleteQuery = new Neo4jQuery(this, entity, getEntityPersister(entity.getJavaClass()));
            deleteQuery.add(Restrictions.in(GormProperties.IDENTITY, ids) );
            final CypherBuilder cypherBuilder = deleteQuery.getBaseQuery();
            buildCascadingDeletes(entity, cypherBuilder);

            final String cypher = cypherBuilder.build();
            final Map<String, Object> idMap = cypherBuilder.getParams();

            if (log.isDebugEnabled()) {
                log.debug("DELETE Cypher [{}] for parameters {}", cypher, idMap);
            }
            graphDatabaseService.execute(cypher, idMap);

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
        if(wasTransactionTerminated()) return;

        final Neo4jTransaction transaction = getTransaction();
        if(transaction != null) {
            if( transaction.getTransactionDefinition().isReadOnly() ) {
                return;
            }
        }
        persistDirtyButUnsavedInstances();
        super.flush();
    }

    @Override
    public Neo4jTransaction getTransaction() {
        return (Neo4jTransaction) super.getTransaction();
    }

    protected boolean wasTransactionTerminated() {
        return transaction != null && !transaction.isActive();
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        super.postFlush(hasUpdates);
        if (publisher!=null) {
            publisher.publishEvent(new SessionFlushedEvent(this));
        }
    }

    public Neo4jTransaction assertTransaction() {
        if(transaction == null || (wasTransactionTerminated() && !TransactionSynchronizationManager.isSynchronizationActive())) {
            // start a new transaction upon termination
            final DefaultTransactionDefinition transactionDefinition = createDefaultTransactionDefinition(null);
            transaction = new Neo4jTransaction(graphDatabaseService, transactionDefinition, true);
        }
        return (Neo4jTransaction) transaction;
    }

    protected DefaultTransactionDefinition createDefaultTransactionDefinition(TransactionDefinition other) {
        final DefaultTransactionDefinition transactionDefinition = other != null ? new DefaultTransactionDefinition(other) : new DefaultTransactionDefinition();
        transactionDefinition.setName(Neo4jTransaction.DEFAULT_NAME);
        return transactionDefinition;
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


    @Override
    public long deleteAll(QueryableCriteria criteria) {

        final PersistentEntity entity = criteria.getPersistentEntity();
        final List<Query.Criterion> criteriaList = criteria.getCriteria();
        final Neo4jQuery query = new Neo4jQuery(this, entity, getEntityPersister(entity.getJavaClass()));
        for (Query.Criterion criterion : criteriaList) {
            query.add(criterion);
        }
        query.projections().count();

        final CypherBuilder baseQuery = query.getBaseQuery();
        buildCascadingDeletes(entity, baseQuery);

        final String cypher = baseQuery.build();
        final Map<String, Object> params = baseQuery.getParams();
        if(log.isDebugEnabled()) {
            log.debug("DELETE Cypher [{}] for parameters [{}]", cypher, params);
        }
        Number count = (Number) query.singleResult();
        graphDatabaseService.execute(cypher, params);
        return count.longValue();
    }

    protected void buildCascadingDeletes(PersistentEntity entity, CypherBuilder baseQuery) {
        int i = 1;
        for (Association association : entity.getAssociations()) {
            if(association.doesCascade(CascadeType.REMOVE)) {

                String a = "a" + i++;
                baseQuery.addOptionalMatch("(n)"+ Neo4jQuery.matchForAssociation(association)+"("+ a +")");
                baseQuery.addDeleteColumn(a);

            }
        }
        baseQuery.addDeleteColumn(CypherBuilder.NODE_VAR);
    }

    @Override
    public long updateAll(QueryableCriteria criteria, Map<String, Object> properties) {
        final PersistentEntity entity = criteria.getPersistentEntity();
        final List<Query.Criterion> criteriaList = criteria.getCriteria();
        final Neo4jQuery query = new Neo4jQuery(this, entity, getEntityPersister(entity.getJavaClass()));
        for (Query.Criterion criterion : criteriaList) {
            query.add(criterion);
        }
        query.projections().count();

        final CypherBuilder baseQuery = query.getBaseQuery();
        baseQuery.addPropertySet(properties);
        baseQuery.addReturnColumn(COUNT_RETURN);

        final String cypher = baseQuery.build();
        final Map<String, Object> params = baseQuery.getParams();
        if(log.isDebugEnabled()) {
            log.debug("UPDATE Cypher [{}] for parameters [{}]", cypher, params);
        }
        final Result execute = graphDatabaseService.execute(cypher, params);
        if(execute.hasNext()) {
            final Map<String, Object> result = IteratorUtil.single(execute);
            return ((Number) result.get(TOTAL_COUNT)).longValue();
        }
        else {
            return 0;
        }
    }

    private static class RelationshipUpdateKey {
        private final Serializable id;
        private final Association association;

        public RelationshipUpdateKey(Serializable id, Association association) {
            this.id = id;
            this.association = association;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RelationshipUpdateKey that = (RelationshipUpdateKey) o;

            if (association != null ? !association.equals(that.association) : that.association != null) return false;
            return !(id != null ? !id.equals(that.id) : that.id != null);

        }

        @Override
        public int hashCode() {
            int result = association != null ? association.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
    }
}


