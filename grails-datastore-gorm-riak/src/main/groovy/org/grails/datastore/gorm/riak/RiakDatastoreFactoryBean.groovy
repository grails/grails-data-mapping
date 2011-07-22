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

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener

import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.riak.RiakDatastore

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakDatastoreFactoryBean implements FactoryBean<RiakDatastore>, ApplicationContextAware {

    Map<String, String> config
    MappingContext mappingContext
    ApplicationContext applicationContext

    RiakDatastore getObject() {
        RiakDatastore datastore = new RiakDatastore(mappingContext, config, applicationContext)
        applicationContext.addApplicationListener new DomainEventListener(datastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)
        datastore.afterPropertiesSet()

        datastore
    }

    Class<?> getObjectType() { RiakDatastore }

    boolean isSingleton() { true }
}
