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
package org.grails.datastore.gorm.mongo.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.gorm.mongo.MongoGormInstanceApi
import org.grails.datastore.gorm.mongo.MongoGormStaticApi
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.springframework.transaction.PlatformTransactionManager

import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import com.mongodb.DBObject

/**
 * Mongo specific dynamic methods configurer.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class MongoMethodsConfigurer extends DynamicMethodsConfigurer{

    MongoMethodsConfigurer(MongoDatastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    void configure() {
        super.configure()

        def asTypeHook = { Class cls ->
            MongoEntityPersister p = datastore.currentSession.getPersister(cls)
            if (p != null) {
                if (delegate instanceof DBCursor) {
                    def mongoResults = new MongoQuery.MongoResultList(delegate,0, p)
                    if (!mongoResults.isEmpty()) {
                        return mongoResults.get(0)
                    }
                    else {
                        return null
                    }
                }
                else {
                    DBObject dbo = delegate
                    def key = dbo.get(MongoEntityPersister.MONGO_ID_FIELD)
                    return p.createObjectFromNativeEntry(p.persistentEntity, key, dbo)
                }
            }
            else {
                if((delegate instanceof DBObject) && cls.name == 'grails.converters.JSON') {
                    return cls.newInstance( delegate.toMap() )
                }
                else {
                    throw new IllegalArgumentException("Cannot convert DBOject [$delegate] to target type $cls. Type is not a persistent entity")
                }
            }
        }
        DBObject.metaClass.asType = asTypeHook
        BasicDBObject.metaClass.asType = asTypeHook
        DBCursor.metaClass.asType = asTypeHook
        DBCursor.metaClass.toList = { Class cls ->
            MongoEntityPersister p = datastore.currentSession.getPersister(cls)
            if (p)
                return new MongoQuery.MongoResultList(delegate,0,p)
            else {
                throw new IllegalArgumentException("Cannot convert DBCursor [$delegate] to target type $cls. Type is not a persistent entity")
            }
        }
    }

    @Override
    String getDatastoreType() {
        return "Mongo"
    }

    @Override
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        new MongoGormStaticApi(cls, datastore, finders)
    }

    @Override
    protected GormInstanceApi createGormInstanceApi(Class cls) {
        final api = new MongoGormInstanceApi(cls, datastore)
        api.failOnError = failOnError
        api
    }

    @Override
    protected GormEnhancer createEnhancer() {
        def ge
        if (transactionManager == null) {
            ge = new MongoGormEnhancer(datastore)
        }
        else {
            ge = new MongoGormEnhancer(datastore, transactionManager)
        }
        ge.failOnError = failOnError
        ge
    }
}
