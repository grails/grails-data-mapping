/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.datastore.mapping.core.AbstractSession
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.engine.Persister
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.Transaction

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jSession extends AbstractSession {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    Neo4jTransaction transaction
    Set persistedIds = []

    Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher)
        log.debug "created new Neo4jSession"
        //beginTransactionInternal()

/*        this.mongoDatastore = datastore
        try {
            getNativeInterface().requestStart()
        }
        catch (IllegalStateException ignored) {
            // can't call authenticate() twice, and it's probably been called at startup
        }*/
    }

    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName())
        return entity == null ? null : new Neo4jEntityPersister(mappingContext, entity, this, publisher)
    }

    protected Transaction beginTransactionInternal() {
        //transaction?.commit()
        transaction = new Neo4jTransaction(nativeInterface)
        transaction
    }

    GraphDatabaseService getNativeInterface() {
        ((Neo4jDatastore)datastore).graphDatabaseService
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        //beginTransactionInternal()
    }

    @Override
    void disconnect() {
        log.debug "disconnect"
        super.disconnect()
        //transaction?.commit()
    }

    def createInstanceForNode(Node node) {
        String className = node.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null)
        PersistentEntity persistentEntity = mappingContext.getPersistentEntity(className)
        if (!persistentEntity) {
            log.warn "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME not set for id=$node.id"
            null
        }

        log.debug "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME = $className for id=$node.id"
        Persister persister = getPersister(persistentEntity)
        assert persister
        def object = persister.retrieve(node.id)
        log.debug "createInstanceForNode: object = $object"
        object
    }

    def createInstanceForNode(long id) {
        createInstanceForNode(nativeInterface.getNodeById(id))
    }

    @Override
    Serializable persist(o) {
        Long id = o.id
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
        objects.collect { persist(it) }
    }

    @Override
    void clear() {
        super.clear()
        persistedIds = []
    }
}
