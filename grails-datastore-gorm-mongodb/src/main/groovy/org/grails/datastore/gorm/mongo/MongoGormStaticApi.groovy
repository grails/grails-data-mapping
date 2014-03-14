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
package org.grails.datastore.gorm.mongo

import com.gmongo.internal.DBCollectionPatcher
import com.mongodb.DB
import com.mongodb.DBCollection
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.mongo.MongoSession
import org.springframework.transaction.PlatformTransactionManager

/**
 * Mongo GORM static level API
 *
 * @author Graeme Rocher
 * @since 1.0
 * @param < D > The domain class type
 */
@CompileStatic
class MongoGormStaticApi<D> extends GormStaticApi<D> {
    MongoGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)
    }

    MongoGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    @Override
    MongoCriteriaBuilder createCriteria() {
        return new MongoCriteriaBuilder(persistentClass, datastore.currentSession)
    }

    /**
     * @return The database for this domain class
     */
    DB getDB() {
        MongoSession ms = (MongoSession)datastore.currentSession
        ms.getMongoTemplate(persistentEntity).getDb()
    }

    /**
     * @return The name of the Mongo collection that entity maps to
     */
    String getCollectionName() {
        MongoSession ms = (MongoSession)datastore.currentSession
        ms.getCollectionName(persistentEntity)
    }

    /**
     * The actual collection that this entity maps to.
     *
     * @return The actual collection
     */
    DBCollection getCollection() {
        MongoSession ms = (MongoSession)datastore.currentSession
        def template = ms.getMongoTemplate(persistentEntity)

        def coll = template.getCollection(ms.getCollectionName(persistentEntity))
        DBCollectionPatcher.patch(coll)
        return coll
    }

    /**
     * Use the given collection for this entity for the scope of the closure call
     * @param collectionName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    def withCollection(String collectionName, Closure callable) {
        MongoSession ms = (MongoSession)datastore.currentSession
        final previous = ms.useCollection(persistentEntity, collectionName)
        try {
            callable.call(ms)
        }
        finally {
            ms.useCollection(persistentEntity, previous)
        }
    }

    /**
     * Use the given collection for this entity for the scope of the session
     *
     * @param collectionName The collection name
     * @return The previous collection name
     */
    String useCollection(String collectionName) {
        MongoSession ms = (MongoSession)datastore.currentSession
        ms.useCollection(persistentEntity, collectionName)
    }

    /**
     * Use the given database for this entity for the scope of the closure call
     * @param databaseName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    def withDatabase(String databaseName, Closure callable) {
        MongoSession ms = (MongoSession)datastore.currentSession
        final previous = ms.useDatabase(persistentEntity, databaseName)
        try {
            callable.call(ms)
        }
        finally {
            ms.useDatabase(persistentEntity, previous)
        }
    }

    /**
     * Use the given database for this entity for the scope of the session
     *
     * @param databaseName The collection name
     * @return The previous database name
     */
    String useDatabase(String databaseName) {
        MongoSession ms = (MongoSession)datastore.currentSession
        ms.useDatabase(persistentEntity, databaseName)
    }
}
