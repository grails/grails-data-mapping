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
    private static final String COUNT_RETURN = "count(n) as total";
    private static final String TOTAL_COUNT = "total";
    private static Logger log = LoggerFactory.getLogger(Neo4jSession.class);
    private static final EvictionListener<RelationshipUpdateKey, Collection<Long>> EXCEPTION_THROWING_INSERT_LISTENER =
            new EvictionListener<RelationshipUpdateKey, Collection<Long>>() {
                public void onEviction(RelationshipUpdateKey association, Collection<Long> value) {
                    throw new DataAccessResourceFailureException("Maximum number (5000) of relationship update operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
                }
            };

    protected Map<RelationshipUpdateKey, Collection<Long>> pendingRelationshipInserts =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Long>>()
                    .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                    .maximumWeightedCapacity(5000).build();

    protected Map<RelationshipUpdateKey, Collection<Long>> pendingRelationshipDeletes =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Long>>()
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
    public void addPendingRelationshipInsert(Long parentId, Association association, Long id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipInserts);
    }

    /**
     * Adds a relationship that is pending deletion
     *
     * @param association The association
     * @param id The id
     */
    public void addPendingRelationshipDelete(Long parentId, Association association, Long id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipDeletes);
    }

    protected void addRelationshipUpdate(Long parentId, Association association, Long id, Map<RelationshipUpdateKey, Collection<Long>> targetMap) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(parentId, association);
        Collection<Long> inserts = targetMap.get(key);
        if (inserts == null) {
            inserts = new ConcurrentLinkedQueue<Long>();
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
                final List<PendingOperation> preOperations = pendingUpdate.getPreOperations();
                executePendings(preOperations);

                pendingUpdate.run();

                if(pendingUpdate.isVetoed()) continue;

                final EntityAccess access = pendingUpdate.getEntityAccess();
                final Collection<PendingOperation> cascadingOperations = new ArrayList<PendingOperation>(pendingUpdate.getCascadeOperations());

                final String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance(access.getEntity());
                final StringBuilder cypherStringBuilder = new StringBuilder();

                final Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
                final Long id = (Long)pendingUpdate.getNativeKey();
                params.put(GormProperties.IDENTITY, id);
                final Map<String, Object> simpleProps = new HashMap<String, Object>();

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
                        executePendings(cascadingOperations);
                    }
                }
            }
        }

    }

    private void processPendingRelationshipUpdates(EntityAccess parent, Long parentId, Association association, Collection<PendingOperation> cascadingOperations) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(parentId, association);
        final Collection<Long> pendingInserts = pendingRelationshipInserts.get(key);
        if(pendingInserts != null) {
            cascadingOperations.add(new RelationshipPendingInsert(parent, association, pendingInserts, graphDatabaseService, true));
        }
        final Collection<Long> pendingDeletes = pendingRelationshipDeletes.get(key);
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
                final Long id = (Long)entityInsert.getNativeKey();
                simpleProps.put(CypherBuilder.IDENTIFIER, id);


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
                    else if(pp instanceof Association) {
                        Association association = (Association) pp;
                        processPendingRelationshipUpdates(access, id, association, cascadingOperations);
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
        persistDirtyButUnsavedInstances();
        super.flush();
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
        private final Long id;
        private final Association association;

        public RelationshipUpdateKey(Long id, Association association) {
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


