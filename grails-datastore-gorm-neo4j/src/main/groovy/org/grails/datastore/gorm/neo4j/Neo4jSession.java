package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.*;
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
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.transactions.SessionHolder;
import org.grails.datastore.mapping.transactions.Transaction;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
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
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {

        Collection<PendingInsert> nodes = new ArrayList<PendingInsert>();
        Collection<PendingInsert> relationships = new ArrayList<PendingInsert>();
        for (Collection<PendingInsert> coll : inserts.values()) {
            for (PendingInsert pendingInsert: coll)  {
                if (pendingInsert instanceof NodePendingInsert) {
                    nodes.add(pendingInsert);
                }
                if ((pendingInsert instanceof RelationshipPendingInsert) ||
                    (pendingInsert instanceof RelationshipPendingDelete)) {
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


