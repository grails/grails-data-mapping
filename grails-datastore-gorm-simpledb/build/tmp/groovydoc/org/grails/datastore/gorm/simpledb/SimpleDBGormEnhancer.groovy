/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.simpledb

import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.springframework.transaction.PlatformTransactionManager

import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.simpledb.engine.SimpleDBNativeItem
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate

/**
 * GORM enhancer for SimpleDB.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
class SimpleDBGormEnhancer extends GormEnhancer {

    SimpleDBGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    SimpleDBGormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new SimpleDBGormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new SimpleDBGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }
}

class SimpleDBGormInstanceApi<D> extends GormInstanceApi<D> {

    SimpleDBGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
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
            getDbo(instance)?.put name, value
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
            return SimpleDBTemplate.get(name)
        }
        return null
    }

    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The NativeSimpleDBItem instance
     */
    SimpleDBNativeItem getDbo(D instance) {
        execute (new SessionCallback<SimpleDBNativeItem>() {
            SimpleDBNativeItem doInSession(Session session) {

                if (!session.contains(instance) && !instance.save()) {
                    throw new IllegalStateException(
                        "Cannot obtain DBObject for transient instance, save a valid instance first")
                }

                EntityPersister persister = session.getPersister(instance)
                def id = persister.getObjectIdentifier(instance)
                return session.getCachedEntry(persister.getPersistentEntity(), id)
            }
        })
    }
}

class SimpleDBGormStaticApi<D> extends GormStaticApi<D> {
    SimpleDBGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)
    }

    SimpleDBGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    @Override
    SimpleDBCriteriaBuilder createCriteria() {
        return new SimpleDBCriteriaBuilder(persistentClass, datastore.currentSession)
    }
}
