package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingOperation;
import org.grails.datastore.mapping.core.impl.PendingOperationExecution;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.transactions.Transaction;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.*;

/**
 * Created by stefan on 03.03.14.
 */
public class Neo4jSession extends AbstractSession<ExecutionEngine> {

    private static Logger log = LoggerFactory.getLogger(Neo4jSession.class);

    private CypherEngine cypherEngine;

    /** map node id -> hashmap of relationship types showing startNode id and endNode id */
    private Collection<Object> persistingInstances = new HashSet<Object>();

    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, CypherEngine cypherEngine) {
        super(datastore, mappingContext, publisher, stateless);
        this.cypherEngine = cypherEngine;
        cypherEngine.beginTx();
    }

    @Override
    public void disconnect() {
        cypherEngine.commit();
        super.disconnect();
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null ) ? new Neo4jEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new Neo4jTransaction(cypherEngine);
    }

    @Override
    public Neo4jDatastore getDatastore() {
        return (Neo4jDatastore) super.getDatastore();
    }

    @Override
    public CypherEngine getNativeInterface() {
        return cypherEngine;
    }

    @Override
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {

        Collection<PendingInsert> nodes = new ArrayList<PendingInsert>();
        Collection<PendingInsert> relationships = new ArrayList<PendingInsert>();
        for (Collection<PendingInsert> coll : inserts.values()) {
            for (PendingInsert pendingInsert: coll)  {
                if (pendingInsert instanceof NodePendingInsert) {
                    nodes.add(pendingInsert);
                }
                if (pendingInsert instanceof RelationshipPendingInsert) {
                    relationships.add(pendingInsert);
                }

            }
        }
        flushPendingOperations(nodes);

        flushPendingOperations(relationships);
    }


    /**
     * TODO: find clean solution instead of copying from base class

     * @param operations
     */
    private void flushPendingOperations(Collection operations) {

        for (Object o : operations) {
            PendingOperation pendingOperation = (PendingOperation) o;
            try {
                PendingOperationExecution.executePendingOperation(pendingOperation);
            } catch (RuntimeException e) {
                setFlushMode(FlushModeType.COMMIT);
                throw new InvalidDataAccessResourceUsageException("Do not flush() the Session after an exception occurs", e);
            }
        }
    }

/*    void setPersistentRelationships(Long id, Map<String, Collection<Collection<Long>>> relationships) {
        persistentRelationships.put(id, relationships)
    }

    Map<String, Collection<Collection<Long>>> getPersistentRelationships(Long id) {
        persistentRelationships.get(id)
    }*/

    @Override
    public void flush() {
        persistDirtyButUnsavedInstances();
        super.flush();
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        persistingInstances.clear();
        super.postFlush(hasUpdates);
        if (publisher!=null) {
            publisher.publishEvent(new SessionFlushedEvent(this));
        }
        cypherEngine.commit();
    }

    /**
     * in case a known instance is modified and not explicitly saved, we track dirty state here and spool them for persisting
     */
    private void persistDirtyButUnsavedInstances() {
        Set pendingObjects = new HashSet();
        for ( Collection<PendingInsert> coll : getPendingInserts().values()) {
            for (PendingInsert pi: coll) {
                pendingObjects.add(pi.getEntityAccess().getEntity());
            }
        }
        for ( Collection<PendingUpdate> coll : getPendingUpdates().values()) {
            for (PendingUpdate pendingUpdate: coll) {
                pendingObjects.add(pendingUpdate.getEntityAccess().getEntity());
            }
        }

        for (Map<Serializable, Object> cache : firstLevelCache.values()) {
            for (Object obj: cache.values()) {
                if (obj instanceof DirtyCheckable) {
                    boolean isDirty = ((DirtyCheckable)obj).hasChanged();
                    if (isDirty && (!pendingObjects.contains(obj))) {
                        persist(obj);
                    }
                }
            }
        }
    }

    public void addPersistingInstance(Object obj) {
        persistingInstances.add(obj);
    }

    public boolean containsPersistingInstance(Object obj) {
        return persistingInstances.contains(obj);
    }

}


