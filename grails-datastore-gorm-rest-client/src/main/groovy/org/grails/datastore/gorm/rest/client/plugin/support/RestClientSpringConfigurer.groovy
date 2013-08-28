/* Copyright (C) 2013 original authors
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
package org.grails.datastore.gorm.rest.client.plugin.support

import org.grails.datastore.mapping.rest.client.config.RestClientMappingContext
import org.grails.datastore.gorm.plugin.support.SpringConfigurer

/**
 * Spring configurer for the GORM REST client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RestClientSpringConfigurer extends SpringConfigurer{
    @Override
    String getDatastoreType() {
        return "RestClient"
    }

    @Override
    Closure getSpringCustomizer() {
        return {
            def restClientConfig = application.config?.grails?.gorm?.restClient.clone()
            final defaultMapping = restClientConfig?.default?.mapping
            restclientMappingContext(RestClientMappingContext, defaultMapping instanceof Closure ? defaultMapping : {})
            restclientDatastore(RestClientDatastoreFactoryBean) {
                mappingContext = ref("restclientMappingContext")
                connectionDetails = restClientConfig
            }
        }
    }
}
