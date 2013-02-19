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
package org.grails.datastore.gorm.dynamodb

import org.grails.datastore.mapping.dynamodb.engine.DynamoDBNativeItem
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.engine.EntityPersister
import org.springframework.transaction.PlatformTransactionManager

/**
 * GORM enhancer for DynamoDB.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
class DynamoDBGormEnhancer extends GormEnhancer {

    DynamoDBGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    DynamoDBGormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new DynamoDBGormStaticApi<D>(cls, datastore, finders)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new DynamoDBGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }
}

class DynamoDBGormInstanceApi<D> extends GormInstanceApi<D> {

    DynamoDBGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
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
            getDbo(instance)?.put name, value, xx
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
            return DynamoDBTemplate.get(name)
        }
        return null
    }

    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The NativeDynamoDBItem instance
     */
    DynamoDBNativeItem getDbo(D instance) {
        execute (new SessionCallback<DynamoDBNativeItem>() {
            DynamoDBNativeItem doInSession(Session session) {

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

class DynamoDBGormStaticApi<D> extends GormStaticApi<D> {
    DynamoDBGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders)
    }

    @Override
    DynamoDBCriteriaBuilder createCriteria() {
        return new DynamoDBCriteriaBuilder(persistentClass, datastore.currentSession)
    }
}
