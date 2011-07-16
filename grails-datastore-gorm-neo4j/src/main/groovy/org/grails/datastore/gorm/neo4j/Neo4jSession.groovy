package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.core.AbstractSession
import org.springframework.datastore.mapping.engine.Persister
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.context.ApplicationEventPublisher
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.Transaction
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.neo4j.graphdb.Node

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 12:25
 * To change this template use File | Settings | File Templates.
 */
class Neo4jSession extends AbstractSession {

    private static final Logger log = LoggerFactory.getLogger(Neo4jSession.class);

    Neo4jTransaction transaction
    def persistedIds = [] as Set

    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        log.debug "created new Neo4jSession"
        //beginTransactionInternal()

/*        this.mongoDatastore = datastore;
        try {
            getNativeInterface().requestStart();
        }
        catch (IllegalStateException ignored) {
            // can't call authenticate() twice, and it's probably been called at startup
        }*/
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new Neo4jEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override
    protected Transaction beginTransactionInternal() {
        //transaction?.commit()
        transaction = new Neo4jTransaction(nativeInterface)
        transaction
    }

    Object getNativeInterface() {
        datastore.graphDatabaseService
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        //beginTransactionInternal()
    }

    @Override
    public void disconnect() {
        log.debug "disconnect"
        super.disconnect()
        //transaction?.commit()
    }

    def createInstanceForNode(Node node) {
        def className = node.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null)
        def persistentEntity = mappingContext.getPersistentEntity(className)
        if (!persistentEntity) {
            log.warn "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME not set for id=$node.id"
            null
        }

        log.debug "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME = $className for id=$node.id"
        def persister = getPersister(persistentEntity)
        assert persister
        def object = persister.retrieve(node.id)
        log.debug "createInstanceForNode: object = $object"
        object
    }

    def createInstanceForNode(long id) {
        createInstanceForNode(nativeInterface.getNodeById(id))
    }

    @Override
    Serializable persist(Object o) {
        def id = o.id
        log.info "persisting $id , persistedIds $persistedIds"
        if (!(id in persistedIds)) {
            if (id) {
                persistedIds << id
                super.persist(o)
            } else {
                id = super.persist(o)
                persistedIds << id
            }
        }
        id
    }

    @Override
    List<Serializable> persist(Iterable objects) {
        objects.collect {
            persist(it)
        }
    }

    @Override
    void clear() {
        super.clear()
        persistedIds = [] as Set
    }


}
