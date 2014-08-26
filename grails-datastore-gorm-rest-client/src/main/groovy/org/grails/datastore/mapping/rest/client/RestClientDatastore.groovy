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
package org.grails.datastore.mapping.rest.client

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.rest.render.RendererRegistry
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.rest.client.config.Endpoint
import org.grails.datastore.mapping.rest.client.config.RestClientMappingContext
import org.grails.datastore.mapping.rest.client.http.converters.PersistentEntityHttpConverter
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.grails.web.databinding.bindingsource.DefaultDataBindingSourceRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.client.RestTemplate

import java.util.concurrent.ConcurrentHashMap
import org.grails.datastore.mapping.rest.client.http.converters.PersistentEntityListHttpConverter

/**
 * Datastore implementation for REST clients, provides some base configuration shared by REST clients
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientDatastore extends AbstractDatastore     {

    public static final String SETTING_BASE_URL = "baseUrl"
    public static final String DEFAULT_BASE_URL = "http://localhost:8080"
    @Autowired(required = false)
    RendererRegistry rendererRegistry

    @Autowired(required = false)
    DataBindingSourceRegistry bindingSourceRegistry

    String baseUrl = DEFAULT_BASE_URL


    Map<PersistentEntity, AsyncRestBuilder> asyncRestClients = new ConcurrentHashMap<>().withDefault { PersistentEntity entity ->
        new AsyncRestBuilder(syncRestClients.get(entity))
    }

    Map<PersistentEntity, RestBuilder> syncRestClients = new ConcurrentHashMap<>().withDefault { PersistentEntity entity ->
        final template = new RestTemplate(((Endpoint) entity.mapping.mappedForm).httpRequestFactory)
        final converters = template.getMessageConverters()
        converters.add(new PersistentEntityHttpConverter<Object>(entity, rendererRegistry, bindingSourceRegistry))
        converters.add(new PersistentEntityListHttpConverter<Object>(entity, rendererRegistry, bindingSourceRegistry))
        new RestBuilder(template)
    }

    RestClientDatastore() {
        this(new RestClientMappingContext())
    }

    RestClientDatastore(RestClientMappingContext mappingContext) {
        this(mappingContext, Collections.emptyMap(), null)
    }

    RestClientDatastore(RestClientMappingContext mappingContext, Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        this(mappingContext, connectionDetails, ctx, null)
    }

    RestClientDatastore(RestClientMappingContext mappingContext, Map<String, String> connectionDetails, ConfigurableApplicationContext ctx, TPCacheAdapterRepository cacheAdapterRepository) {
        super(mappingContext, connectionDetails, ctx, cacheAdapterRepository)

        initialize(connectionDetails)
    }

    protected void initialize(Map<String, String> connectionDetails) {
        DefaultRendererRegistry defaultRendererRegistry = new DefaultRendererRegistry()
        defaultRendererRegistry.initialize()
        rendererRegistry = defaultRendererRegistry
        final defaultDataBindingSourceRegistry = new DefaultDataBindingSourceRegistry()
        defaultDataBindingSourceRegistry.initialize()

        bindingSourceRegistry = defaultDataBindingSourceRegistry

        baseUrl = connectionDetails.get(SETTING_BASE_URL) ?: DEFAULT_BASE_URL
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new RestClientSession(this, mappingContext, applicationEventPublisher, cacheAdapterRepository)
    }
}
