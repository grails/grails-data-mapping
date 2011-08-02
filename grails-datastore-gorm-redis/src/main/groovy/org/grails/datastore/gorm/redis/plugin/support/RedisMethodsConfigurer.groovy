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
package org.grails.datastore.gorm.redis.plugin.support

import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.redis.RedisGormEnhancer
import org.grails.datastore.gorm.redis.RedisGormInstanceApi
import org.grails.datastore.gorm.redis.RedisGormStaticApi

/**
 * Methods configurer for Redis
 */
class RedisMethodsConfigurer extends DynamicMethodsConfigurer{

    RedisMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "Redis"
    }

    @Override
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new RedisGormStaticApi(cls, datastore, finders)
    }

    @Override
    protected GormInstanceApi createGormInstanceApi(Class cls) {
        return new RedisGormInstanceApi(cls, datastore)
    }

    @Override
    protected GormEnhancer createEnhancer() {
        if(transactionManager != null) {
            return new RedisGormEnhancer(datastore, transactionManager)
        }
        else {
            return new RedisGormEnhancer(datastore)
        }
    }


}
