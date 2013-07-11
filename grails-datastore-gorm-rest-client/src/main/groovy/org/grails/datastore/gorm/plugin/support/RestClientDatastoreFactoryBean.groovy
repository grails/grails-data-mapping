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
package org.grails.datastore.gorm.plugin.support

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.grails.datastore.mapping.rest.client.config.RestClientMappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext

/**
 * Spring Factory bean for constructing the RestClientDatastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientDatastoreFactoryBean implements FactoryBean<org.grails.datastore.mapping.rest.client.RestClientDatastore>, ApplicationContextAware, InitializingBean{
    ApplicationContext applicationContext
    RestClientMappingContext mappingContext
    Map connectionDetails
    @Autowired(required = false)
    TPCacheAdapterRepository cacheAdapterRepository

    private RestClientDatastore restClientDatastore

    @Override
    RestClientDatastore getObject() throws Exception {
        restClientDatastore
    }

    @Override
    Class<?> getObjectType() { RestClientDatastore }

    @Override
    boolean isSingleton() { true }

    @Override
    void afterPropertiesSet() throws Exception {
        restClientDatastore = new RestClientDatastore(mappingContext, connectionDetails, (ConfigurableApplicationContext)applicationContext, cacheAdapterRepository)
    }
}
