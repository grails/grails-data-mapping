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
import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBCollection
import com.mongodb.DBObject
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

/**
 * GORM enhancer for Mongo.
 *
 * @author Graeme Rocher
 */
class MongoGormEnhancer extends GormEnhancer {

    MongoGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)

        DynamicFinder.registerNewMethodExpression(Near)
        DynamicFinder.registerNewMethodExpression(WithinBox)
        DynamicFinder.registerNewMethodExpression(WithinPolygon)
        DynamicFinder.registerNewMethodExpression(WithinCircle)
    }

    MongoGormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new MongoGormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new MongoGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }
}

class MongoGormInstanceApi<D> extends GormInstanceApi<D> {

    MongoGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    def propertyMissing(D instance, String name) {
        getAt(instance, name)
    }

    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    def propertyMissing(D instance, String name, val) {
        putAt(instance, name, val)
    }
    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(D instance, String name, value) {
        if (instance.hasProperty(name)) {
            instance.setProperty(name, value)
        }
        else {
            execute (new SessionCallback<DBObject>() {
                DBObject doInSession(Session session) {
                    SessionImplementor si = (SessionImplementor)session

                    if (si.isStateless(persistentEntity)) {
                        MongoDatastore ms = (MongoDatastore)datastore
                        def template = ms.getMongoTemplate(persistentEntity)

                        def coll = template.getCollection(ms.getCollectionName(persistentEntity))
                        MongoEntityPersister persister = session.getPersister(instance)
                        def id = persister.getObjectIdentifier(instance)
                        final updateObject = new BasicDBObject('$set', new BasicDBObject(name, value))
                        coll.update(new BasicDBObject(MongoEntityPersister.MONGO_ID_FIELD,id), updateObject)
                        return updateObject
                    }
                    else {
                        final dbo = getDbo(instance)
                        dbo?.put name, value
                        if (instance instanceof DirtyCheckable) {
                            ((DirtyCheckable)instance).markDirty()
                        }
                        return dbo
                    }
                }
            })

        }
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    def getAt(D instance, String name) {
        if (instance.hasProperty(name)) {
            return instance.getProperty(name)
        }

        def dbo = getDbo(instance)
        if (dbo != null && dbo.containsField(name)) {
            return dbo.get(name)
        }
        return null
    }

    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The DBObject instance
     */
    DBObject getDbo(D instance) {
        execute (new SessionCallback<DBObject>() {
            DBObject doInSession(Session session) {

                if (!session.contains(instance) && !instance.save()) {
                    throw new IllegalStateException(
                        "Cannot obtain DBObject for transient instance, save a valid instance first")
                }

                MongoEntityPersister persister = session.getPersister(instance)
                def id = persister.getObjectIdentifier(instance)
                def dbo = session.getCachedEntry(persister.getPersistentEntity(), id)
                if (dbo == null) {
                    MongoDatastore ms = (MongoDatastore)datastore
                    def template = ms.getMongoTemplate(persistentEntity)

                    def coll = template.getCollection(ms.getCollectionName(persistentEntity))
                    dbo = coll.findOne( id )

                }
                return dbo
            }
        })
    }
}

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
