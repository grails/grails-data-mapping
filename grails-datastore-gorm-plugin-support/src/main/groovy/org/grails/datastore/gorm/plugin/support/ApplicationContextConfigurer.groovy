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
package org.grails.datastore.gorm.plugin.support

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.core.Datastore
import org.springframework.context.ConfigurableApplicationContext

/**
 * Common logic for the configuration of the ApplicationContext.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ApplicationContextConfigurer {

    String datastoreType

    ApplicationContextConfigurer(String datastoreType) {
        this.datastoreType = datastoreType
    }

    void configure(ConfigurableApplicationContext ctx) {
        final datastore = ctx.getBean("${datastoreType.toLowerCase()}Datastore", Datastore)
        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)
    }
}
