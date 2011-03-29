/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *     Portions (c) 2010 by NPC International, Inc. or the
 *     original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.gorm.riak

import org.grails.datastore.gorm.events.AutoTimestampInterceptor
import org.grails.datastore.gorm.events.DomainEventInterceptor
import org.springframework.beans.factory.FactoryBean
import org.springframework.datastore.mapping.riak.RiakDatastore
import org.springframework.datastore.mapping.model.MappingContext

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakDatastoreFactoryBean implements FactoryBean<RiakDatastore> {

    Map<String, String> config
    MappingContext mappingContext

    RiakDatastore getObject() {
        RiakDatastore datastore = new RiakDatastore(mappingContext, config)
        datastore.addEntityInterceptor(new DomainEventInterceptor())
        datastore.addEntityInterceptor(new AutoTimestampInterceptor())
        datastore.afterPropertiesSet()

        datastore
    }

    Class<?> getObjectType() { RiakDatastore }

    boolean isSingleton() { true }
}
