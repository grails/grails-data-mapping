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
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.mongo.MongoSession
import org.springframework.transaction.PlatformTransactionManager

/**
 * MongoDB GORM static level API
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
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            ms.getMongoTemplate(persistentEntity).getDb()
        } as SessionCallback<DB>)
    }

    /**
     * @return The name of the Mongo collection that entity maps to
     */
    String getCollectionName() {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            ms.getCollectionName(persistentEntity)
        } as SessionCallback<String>)
    }

    /**
     * The actual collection that this entity maps to.
     *
     * @return The actual collection
     */
    DBCollection getCollection() {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            def template = ms.getMongoTemplate(persistentEntity)

            def coll = template.getCollection(ms.getCollectionName(persistentEntity))
            DBCollectionPatcher.patch(coll)
            return coll
        } as SessionCallback<DBCollection>)
    }

    /**
     * Use the given collection for this entity for the scope of the closure call
     * @param collectionName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    def withCollection(String collectionName, Closure callable) {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            final previous = ms.useCollection(persistentEntity, collectionName)
            try {
                callable.call(ms)
            }
            finally {
                ms.useCollection(persistentEntity, previous)
            }

        } as SessionCallback)
    }

    /**
     * Use the given collection for this entity for the scope of the session
     *
     * @param collectionName The collection name
     * @return The previous collection name
     */
    String useCollection(String collectionName) {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            ms.useCollection(persistentEntity, collectionName)
        } as SessionCallback<String>)
    }

    /**
     * Use the given database for this entity for the scope of the closure call
     * @param databaseName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    def withDatabase(String databaseName, Closure callable) {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            final previous = ms.useDatabase(persistentEntity, databaseName)
            try {
                callable.call(ms)
            }
            finally {
                ms.useDatabase(persistentEntity, previous)
            }
        } as SessionCallback)
    }

    /**
     * Use the given database for this entity for the scope of the session
     *
     * @param databaseName The collection name
     * @return The previous database name
     */
    String useDatabase(String databaseName) {
        execute( { Session session ->
            MongoSession ms = (MongoSession)session
            ms.useDatabase(persistentEntity, databaseName)
        } as SessionCallback<String>)
    }
}
