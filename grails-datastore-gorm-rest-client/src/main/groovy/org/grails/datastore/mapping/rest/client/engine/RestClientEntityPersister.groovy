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
package org.grails.datastore.mapping.rest.client.engine

import grails.async.Promise
import grails.async.PromiseList
import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.grails.datastore.mapping.rest.client.config.Endpoint
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus

import java.util.concurrent.TimeUnit
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.rest.client.config.RestClientMappingContext
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.rest.client.RestClientException

/**
 *
 *  EntityPersister implementation for the REST client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientEntityPersister extends EntityPersister {
    RestClientEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        return retrieveAllEntities(pe, Arrays.asList(keys))
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        Endpoint endpoint = (Endpoint)pe.mapping.mappedForm
        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
        List results = []

        MimeType mimeType = new MimeType(endpoint.contentType)
        List<RestResponse> responseResults = []
        if(endpoint.async) {
            PromiseList<RestResponse> promiseList = new PromiseList<>()
            AsyncRestBuilder asyncRestBuilder = datastore.asyncRestClients.get(pe)

            int count = 0
            for(objectId in keys) {
                count++
                final url = establishUrl(datastore.baseUrl, endpoint, pe, objectId)
                promiseList << asyncRestBuilder.get(url) {
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                }
            }

            if(endpoint.readTimeout > -1) {
                responseResults = promiseList.get(endpoint.readTimeout * count, TimeUnit.SECONDS)
            }
            else {
                responseResults = promiseList.get()
            }
        }
        else {
            RestBuilder builder = datastore.syncRestClients.get(pe)
            for(objectId in keys) {
                final url = establishUrl(datastore.baseUrl, endpoint, pe, objectId)
                responseResults <<  builder.get(url) {
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                }
            }
        }
        for(RestResponse response in responseResults) {
            if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                results << bindResponseToNewEntity(response, datastore, mimeType, pe)
            }
            else {
                results << null
            }
        }

        return results
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity entity, @SuppressWarnings("rawtypes") Iterable objs) {
        Endpoint endpoint = (Endpoint)entity.mapping.mappedForm
        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
        if(endpoint.async) {
            final objectList = objs.toList()
            List<Serializable> identifiers = []
            Map updatesAndInserts = objectList.groupBy { getObjectIdentifier(it) ? 'updates' : 'inserts' }
            SessionImplementor impl = (SessionImplementor)getSession()
            Collection updates = (Collection)updatesAndInserts.get("updates")
            if(updates) {

                for(Object update in updates) {
                    identifiers[objectList.indexOf(update)] = getObjectIdentifier(update)
                }

                impl.addPendingUpdate(new PendingUpdateAdapter<AsyncRestBuilder, Object>(entity, null, datastore.asyncRestClients.get(entity), null) {
                    @Override
                    void run() {
                        AsyncRestBuilder builder = getNativeEntry()
                        PromiseList<RestResponse> updateResponses = new PromiseList<RestResponse>()
                        int count = 0
                        for(update in updates) {
                            count++
                            def identifier = identifiers[objectList.indexOf(update)]
                            final url = establishUrl(datastore.baseUrl, endpoint, entity, identifier)
                            updateResponses << builder.put(url) {
                                contentType endpoint.contentType
                                accept endpoint.acceptType ?: entity.javaClass, endpoint.accept
                                body update
                            }
                        }

                        List<RestResponse> responses
                        if(endpoint.readTimeout > -1)
                            responses = updateResponses.get(endpoint.readTimeout * count, TimeUnit.SECONDS)
                        else
                            responses = updateResponses.get()
                        for(RestResponse response in responses) {
                            if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                                throw new RestClientException("Status code [${response.statusCode}] returned for PUT request", response)
                            }
                        }
                    }
                })
            }

            Collection inserts = (Collection)updatesAndInserts.get("inserts")

            if(inserts) {
                AsyncRestBuilder restBuilder = datastore.asyncRestClients.get(entity)
                PromiseList<RestResponse> insertResponses = new PromiseList<RestResponse>()
                int count = 0
                for(insert in inserts) {
                    count++
                    final url = establishUrl(datastore.baseUrl, endpoint, entity)
                    insertResponses << restBuilder.post(url) {
                        contentType endpoint.contentType
                        accept endpoint.acceptType ?: entity.javaClass, endpoint.accept
                        body insert
                    }
                }

                List<RestResponse> responses
                if(endpoint.readTimeout > -1)
                    responses = insertResponses.get(endpoint.readTimeout * count, TimeUnit.SECONDS)
                else
                    responses = insertResponses.get()

                responses.eachWithIndex { RestResponse response, int index ->
                    if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new RestClientException("Status code [${response.statusCode}] returned for POST request", response)
                    }

                    def object = inserts[index]
                    final identifier = getObjectIdentifierFromResponseBody(datastore, response, new MimeType(endpoint.accept), entity, object)
                    identifiers[objectList.indexOf(object)] = identifier
                }
            }


            return identifiers
        }
        else {
            return objs.collect { persistEntity(entity, it) }
        }
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        Endpoint endpoint = (Endpoint)pe.mapping.mappedForm
        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore

        final url = establishUrl(datastore.baseUrl, endpoint, pe, key)
        MimeType mimeType = new MimeType(endpoint.contentType)
        RestResponse response = executeGet(endpoint, datastore, pe, url)

        if(response.statusCode == HttpStatus.OK) {
            return bindResponseToNewEntity(response, datastore, mimeType, pe)
        }
        return null
    }

    private RestResponse executeGet(Endpoint endpoint, RestClientDatastore datastore, PersistentEntity pe, String url) {
        RestResponse response
        if (endpoint.async) {
            AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
            Promise<RestResponse> promise = builder.get(url) {
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
            }
            if (endpoint.readTimeout > -1) {
                response = promise.get(endpoint.readTimeout, TimeUnit.SECONDS)
            }
            else {
                response = promise.get()
            }
        }
        else {
            RestBuilder builder = datastore.syncRestClients.get(pe)
            response = builder.get(url) {
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
            }
        }
        response
    }

    private Object bindResponseToNewEntity(RestResponse response, RestClientDatastore datastore, MimeType mimeType, PersistentEntity pe) {
        final body = response.body
        if(body) {
            if(pe.javaClass.isInstance(body)) {
                return body
            }
            else {
                final bindingSourceRegistry = datastore.bindingSourceRegistry
                DataBindingSource bindingSource = body ? bindingSourceRegistry.createDataBindingSource(mimeType, pe.javaClass, body) : null
                Object instance = null
                if (bindingSource && bindingSource.hasIdentifier()) {
                    instance = pe.newInstance()
                    instance[pe.getIdentity().name] = mappingContext.getConversionService().convert(bindingSource.getIdentifierValue(), pe.getIdentity().getType())

                    DataBindingUtils.bindObjectToInstance(instance, bindingSource)
                }
                return instance
            }
        }
        return null
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        Endpoint endpoint = (Endpoint)pe.mapping.mappedForm

        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore


        final identifier = getObjectIdentifier(obj)
        if(identifier) {
            // do update
            SessionImplementor impl = (SessionImplementor)getSession()
            if(endpoint.async) {
                impl.addPendingUpdate(new PendingUpdateAdapter<AsyncRestBuilder, Object>(pe, identifier, datastore.asyncRestClients.get(pe), new EntityAccess(pe, obj)) {
                    @Override
                    void run() {
                        AsyncRestBuilder builder = getNativeEntry()
                        final url = establishUrl(datastore.baseUrl, endpoint, pe, identifier)
                        Promise<RestResponse> responsePromise = builder.put(url) {
                            contentType endpoint.contentType
                            accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                            body obj
                        }
                        RestResponse response
                        if(endpoint.readTimeout > -1)
                            response = responsePromise.get(endpoint.readTimeout, TimeUnit.SECONDS)
                        else
                            response = responsePromise.get()
                        if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                            throw new RestClientException("Status code [${response.statusCode}] returned for PUT request to URL [$url]", response)
                        }
                    }
                })
            }
            else {
                RestBuilder restBuilder = datastore.syncRestClients.get(pe)

                final url = establishUrl(datastore.baseUrl, endpoint, pe, identifier)
                final RestResponse response = restBuilder.put(url) {
                    contentType endpoint.contentType
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                    body obj
                }
                if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                    throw new RestClientException("Status code [${response.statusCode}] returned for PUT request to URL [$url]", response)
                }
            }
        }
        else {
            return executeInsert(datastore, endpoint, pe, obj)
        }

        return identifier
    }

    @Override
    Serializable insert(Object obj) {
        RestClientMappingContext mappingContext = (RestClientMappingContext )getMappingContext()
        final entity = mappingContext.getPersistentEntity(obj.getClass().name)
        Endpoint endpoint = (Endpoint)entity.mapping.mappedForm

        final identifier = getObjectIdentifier(obj)
        if(identifier) {
            RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
            SessionImplementor impl = (SessionImplementor)getSession()
            impl.addPendingInsert(new PendingInsertAdapter<Object, Object>(entity, identifier, datastore.asyncRestClients.get(entity), new EntityAccess(entity, obj)) {

                @Override
                void run() {
                    executeInsert(datastore,endpoint, entity, obj)
                }
            })
            return identifier
        }
        else {
            return executeInsert((RestClientDatastore)getSession().datastore, endpoint, entity, obj)
        }
    }

    public Serializable executeInsert(RestClientDatastore datastore, Endpoint endpoint, PersistentEntity pe, obj) {
        final url = establishUrl(datastore.baseUrl, endpoint, pe)
        RestResponse response
        MimeType mimeType = new MimeType(endpoint.contentType)
        if (endpoint.async) {
            AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
            Promise<RestResponse> result = builder.post(url) {
                contentType endpoint.contentType
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                body obj
            }
            if (endpoint.readTimeout > -1) {
                response = result.get(endpoint.readTimeout, TimeUnit.SECONDS)
            }
            else {
                response = result.get()
            }
        } else {
            RestBuilder builder = datastore.syncRestClients.get(pe)
            response = builder.post(url) {
                contentType endpoint.contentType
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                body obj
            }
        }

        return getObjectIdentifierFromResponseBody(datastore, response, mimeType, pe, obj)
    }

    protected Serializable getObjectIdentifierFromResponseBody(RestClientDatastore datastore, RestResponse response, MimeType mimeType, PersistentEntity pe, obj) {
        final bindingSourceRegistry = datastore.bindingSourceRegistry
        final body = response.body
        if(body) {
            if(pe.isInstance(body)) {
                final identifier = getObjectIdentifier(body)
                obj[pe.getIdentity().name] = identifier
                return identifier
            }
            else {
                DataBindingSource bindingSource = body ? bindingSourceRegistry.createDataBindingSource(mimeType, pe.javaClass, body) : null
                if (bindingSource) {
                    final objectId = (Serializable) bindingSource.getIdentifierValue()
                    if (objectId) {
                        Serializable convertedId = (Serializable)mappingContext.getConversionService().convert(objectId, pe.getIdentity().getType())
                        obj[pe.getIdentity().name] = convertedId
                        return convertedId
                    }
                    return objectId
                }
                else {
                    return getObjectIdentifier(obj)
                }
            }

        }
        else {
            return getObjectIdentifier(obj)
        }
    }

    String establishUrl(String baseUrl, Endpoint endpoint, PersistentEntity entity, Object identifier) {
        final url = establishUrl(baseUrl, endpoint, entity)
        return "$url/$identifier"
    }

    String establishUrl(String baseUrl, Endpoint endpoint, PersistentEntity entity) {
        if(endpoint.url) {
            return endpoint.url
        }
        else {
            if(endpoint.uri) {
                return new URL(new URL(baseUrl), endpoint.uri).toString()
            }
            else {
                return new URL(new URL(baseUrl), "/${GrailsNameUtils.getPropertyName(entity.javaClass)}").toString()
            }
        }
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object object) {
        if(object) {
            final id = getObjectIdentifier(object)
            if(id) {
                Endpoint endpoint = (Endpoint)pe.mapping.mappedForm
                RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
                final url = establishUrl(datastore.baseUrl, endpoint, pe, id)
                if(endpoint.async) {
                    AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
                    Promise<RestResponse> result = builder.delete(url)
                    RestResponse response
                    if(endpoint.readTimeout>-1) {
                        response = result.get(endpoint.readTimeout, TimeUnit.SECONDS)
                    }
                    else {
                        response = result.get()
                    }
                    if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new RestClientException("Invalid status code [${response.statusCode}] returned for DELETE request to URL [$url]", response)
                    }
                } else {
                    RestBuilder builder = datastore.syncRestClients.get(pe)
                    RestResponse response = builder.delete(url)
                    if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new RestClientException("Invalid status code [${response.statusCode}] returned for DELETE request to URL [$url]", response)
                    }
                }
            }
        }

    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        Endpoint endpoint = (Endpoint)pe.mapping.mappedForm
        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
        if(endpoint.async) {
            AsyncRestBuilder restBuilder = datastore.asyncRestClients.get(pe)
            PromiseList<RestResponse> promiseList = new PromiseList<RestResponse>()
            int count = 0
            for(obj in objects) {
                count++
                final id = getObjectIdentifier(obj)
                if(id) {
                    final url = establishUrl(datastore.baseUrl, endpoint, pe, id)
                    promiseList << restBuilder.delete(url)
                }
            }
            List<RestResponse> responses
            if(endpoint.readTimeout>-1) {
                responses = promiseList.get(endpoint.readTimeout*count, TimeUnit.SECONDS)
            }
            else {
                responses = promiseList.get()
            }

            for(RestResponse response in responses) {
                if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                    throw new RestClientException("Invalid status code [${response.statusCode}] returned for DELETE request. Message", response)
                }
            }
        }
        else {
            for(o in objects) {
                deleteEntity(pe, o)
            }
        }
    }

    @Override
    Query createQuery() {
        throw new UnsupportedOperationException("Querying not implemented")
    }

    @Override
    Serializable refresh(Object o) {
        if(o == null) return null

        final entity = getMappingContext().getPersistentEntity(o.getClass().name)

        if(entity == null) throw new IllegalArgumentException("Argument [$o] is not a persistent entity")

        Endpoint endpoint = (Endpoint)entity.mapping.mappedForm
        RestClientDatastore datastore = (RestClientDatastore)getSession().datastore


        final id = getObjectIdentifier(o)
        if(id == null) return null

        final url = establishUrl(datastore.baseUrl, endpoint, entity, id)
        MimeType mimeType = new MimeType(endpoint.contentType)
        RestResponse response = executeGet(endpoint, datastore, entity, url)

        if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
            final body = response.body
            final bindingSourceRegistry = datastore.bindingSourceRegistry
            DataBindingSource bindingSource = body ? bindingSourceRegistry.createDataBindingSource(mimeType, entity.javaClass, body) : null
            if (bindingSource) {
                DataBindingUtils.bindObjectToInstance(o, bindingSource)
            }
        }
        return (Serializable)id
    }
}
