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

    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        log.info("new")
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
        log.info "disconnect"
        super.disconnect()
        //transaction?.commit()
    }


}
