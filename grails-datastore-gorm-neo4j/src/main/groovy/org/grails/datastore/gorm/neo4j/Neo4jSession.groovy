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
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.util.Assert

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
        PersistentEntity persistentEntity = className ? mappingContext.getPersistentEntity(className) : null
        if (!persistentEntity) {
            log.warn "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME not set for id=$node.id"
            Neo4jUtils.dumpNode(node, log)
            return null
        }

        log.debug "createInstanceForNode: node property $Neo4jEntityPersister.TYPE_PROPERTY_NAME = $className for id=$node.id"
        Persister persister = getPersister(persistentEntity)
        Assert.notNull persister
        def object = persister.retrieve(node.id)
        log.debug "createInstanceForNode: object = $object"
        object
    }

    def createInstanceForNode(long id) {
        createInstanceForNode(nativeInterface.getNodeById(id))
    }

}
