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
package org.grails.datastore.gorm.riak.plugin.support

import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.grails.datastore.gorm.riak.RiakMappingContextFactoryBean
import org.grails.datastore.gorm.riak.RiakDatastoreFactoryBean
import org.springframework.data.keyvalue.riak.groovy.RiakBuilder
import org.springframework.data.keyvalue.riak.core.AsyncRiakTemplate
import org.springframework.data.keyvalue.riak.core.RiakTemplate

/**
 * Configures Spring for Riak
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RiakSpringConfigurer extends SpringConfigurer{
    @Override
    String getDatastoreType() { "Riak" }

    @Override
    Closure getSpringCustomizer() {
        return {
            def riakConfig = application.config?.grails?.riak

            riakDatastoreMappingContext(RiakMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
                pluginManager = ref('pluginManager')
            }

            riakDatastore(RiakDatastoreFactoryBean) {
                config = riakConfig
                mappingContext = ref("riakDatastoreMappingContext")
                pluginManager = ref('pluginManager')
            }

            riakTemplate(RiakTemplate) { bean ->
                def riakDefaultUri = riakConfig?.remove("defaultUri")
                if (riakDefaultUri) {
                    defaultUri = riakDefaultUri
                }
                def riakMapRedUri = riakConfig?.remove("mapReduceUri")
                if (riakMapRedUri) {
                    mapReduceUri = riakMapRedUri
                }
                def riakUseCache = riakConfig?.remove("useCache")
                if (null != riakUseCache) {
                    useCache = riakUseCache
                }
            }

            asyncRiakTemplate(AsyncRiakTemplate) { bean ->
                def riakDefaultUri = riakConfig?.remove("defaultUri")
                if (riakDefaultUri) {
                    defaultUri = riakDefaultUri
                }
                def riakMapRedUri = riakConfig?.remove("mapReduceUri")
                if (riakMapRedUri) {
                    mapReduceUri = riakMapRedUri
                }
                def riakUseCache = riakConfig?.remove("useCache")
                if (riakUseCache) {
                    useCache = riakUseCache
                }
            }

            riak(RiakBuilder, asyncRiakTemplate) { bean ->
                bean.scope = "prototype"
            }
        }
    }
}
