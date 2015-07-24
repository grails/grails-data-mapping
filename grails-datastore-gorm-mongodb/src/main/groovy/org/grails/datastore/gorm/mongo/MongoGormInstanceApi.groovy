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

import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.core.VoidSessionCallback
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.engine.AbstractMongoObectEntityPersister
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister

/**
 * Mongo GORM instance level API
 *
 * @author Graeme Rocher
 * @since 1.0
 * @param < D > The domain class type
 */
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
            setProperty(instance, name, value)
        }
        else {
            execute ( { MongoSession session ->
                SessionImplementor si = (SessionImplementor)session

                if (si.isStateless(persistentEntity)) {
                    def coll = session.getCollection(persistentEntity)
                    MongoEntityPersister persister = (MongoEntityPersister)session.getPersister(instance)
                    def id = persister.getObjectIdentifier(instance)
                    final updateObject = new Document('$set', new Document(name, value))
                    coll.update(new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD,id), updateObject)
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
            } as VoidSessionCallback)

        }
    }

    protected void setProperty(D instance, String name, value) {
        instance.setProperty(name, value)
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
            return getProperty(instance, name)
        }

        def dbo = getDbo(instance)
        if (dbo != null && dbo.containsKey(name)) {
            return dbo.get(name)
        }
        return null
    }

    protected def getProperty(D instance, String name) {
        return instance.getProperty(name)
    }

    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The DBObject instance
     */
    @CompileStatic
    Document getDbo(D instance) {
        execute( { MongoSession session ->
            // check first for embedded cached entries
            SessionImplementor<Document> si = (SessionImplementor<Document>) session;
            def dbo = si.getCachedEntry(persistentEntity, MongoEntityPersister.createEmbeddedCacheEntryKey(instance))
            if(dbo != null) return dbo
            // otherwise check if instance is contained within session
            if (!session.contains(instance)) {
                dbo = new Document()
                si.cacheEntry(persistentEntity, MongoEntityPersister.createInstanceCacheEntryKey(instance), dbo)
                return dbo
            }

            EntityPersister persister = (EntityPersister)session.getPersister(instance)
            def id = persister.getObjectIdentifier(instance)
            dbo = ((SessionImplementor)session).getCachedEntry(persister.getPersistentEntity(), id)
            if (dbo == null) {
                com.mongodb.client.MongoCollection<Document> coll = session.getCollection(persistentEntity)
                dbo = coll.find((Bson)new Document(MongoEntityPersister.MONGO_ID_FIELD, id))
                            .limit(1)
                            .first()

            }
            return dbo
        } as SessionCallback<Document> )
    }
}