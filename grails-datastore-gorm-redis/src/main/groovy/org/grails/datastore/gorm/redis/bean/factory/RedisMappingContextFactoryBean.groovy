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
package org.grails.datastore.gorm.redis.bean.factory

import grails.core.GrailsDomainClass
import grails.redis.RedisEntity
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean

/**
 * Configures the Redis mapping context
 *
 * @author Graeme Rocher
 */
class RedisMappingContextFactoryBean extends AbstractMappingContextFactoryBean {
    protected MappingContext createMappingContext() {
        new KeyValueMappingContext("")
    }

    @Override
    boolean isCompatibleDomainClass(GrailsDomainClass domainClass) {
        return RedisEntity.isAssignableFrom(domainClass.clazz)
    }
}
