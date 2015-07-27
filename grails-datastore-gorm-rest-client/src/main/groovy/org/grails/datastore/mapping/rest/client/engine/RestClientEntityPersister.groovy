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
import grails.core.GrailsDomainClassProperty
import grails.databinding.DataBindingSource
import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.util.GrailsNameUtils
import grails.web.databinding.DataBindingUtils
import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
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
import org.grails.datastore.mapping.engine.BeanEntityAccess
import org.grails.datastore.mapping.rest.client.config.RestClientMappingContext
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.rest.client.RestClientException
import org.grails.datastore.mapping.rest.client.RestClientSession
import org.grails.datastore.mapping.rest.client.query.RequestParameterRestClientQuery

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
        final sessionUrl = session.getAttribute(pe, RestClientSession.ATTRIBUTE_URL)
        final sessionRequestCustomizer = session.getAttribute(pe, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)

        List results = []

        MimeType mimeType = new MimeType(endpoint.contentType)
        List<RestResponse> responseResults = []
        if(endpoint.async) {
            PromiseList<RestResponse> promiseList = new PromiseList<>()
            AsyncRestBuilder asyncRestBuilder = datastore.asyncRestClients.get(pe)

            int count = 0
            for(objectId in keys) {
                count++
                final url = sessionUrl ? sessionUrl.toString() : establishUrl(datastore.baseUrl, endpoint, pe, objectId)
                Map<String, Object> args = [:]
                args.put(GrailsDomainClassProperty.IDENTITY, objectId)

                promiseList << asyncRestBuilder.get(url, args) {
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                    for (entry in endpoint.headers.entrySet()) {
                        header entry.key, entry.value
                    }
                    if(sessionRequestCustomizer instanceof Closure) {
                        Closure callable = (Closure)sessionRequestCustomizer
                        callable.delegate = delegate
                        callable.call()
                    }

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
                final url = sessionUrl ? sessionUrl.toString() : establishUrl(datastore.baseUrl, endpoint, pe, objectId)
                Map<String, Object> args = [:]
                args.put(GrailsDomainClassProperty.IDENTITY, objectId)

                responseResults <<  builder.get(url, args) {
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                    for (entry in endpoint.headers.entrySet()) {
                        header entry.key, entry.value
                    }
                    if(sessionRequestCustomizer instanceof Closure) {
                        Closure callable = (Closure)sessionRequestCustomizer
                        callable.delegate = delegate
                        callable.call()
                    }
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

                RestClientEntityPersister entityPersister = this
                impl.addPendingUpdate(new PendingUpdateAdapter<AsyncRestBuilder, Object>(entity, null, datastore.asyncRestClients.get(entity), null) {
                    @Override
                    void run() {
                        AsyncRestBuilder builder = getNativeEntry()
                        PromiseList<RestResponse> updateResponses = new PromiseList<RestResponse>()
                        int count = 0
                        for(update in updates) {
                            final entityAccess = new BeanEntityAccess(entity, update)
                            if( cancelUpdate(entity, entityAccess) ) continue
                            if( (update instanceof DirtyCheckable) && !getSession().isDirty(update) ) continue

                            count++
                            def identifier = identifiers[objectList.indexOf(update)]
                            final url = establishUrl(datastore.baseUrl, endpoint, entity, identifier)
                            Promise<RestResponse> updatePromise = builder.put(url) {
                                contentType endpoint.contentType
                                accept endpoint.acceptType ?: entity.javaClass, endpoint.accept
                                for (entry in endpoint.headers.entrySet()) {
                                    header entry.key, entry.value
                                }
                                body update
                            }.then { RestResponse response ->
                                if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                                    entityPersister.firePostUpdateEvent(entity, entityAccess)
                                }
                                return response
                            }

                            updateResponses << updatePromise
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
                inserts = inserts.findAll() { !cancelInsert(entity, new BeanEntityAccess(entity, it)) }
                for(insert in inserts) {
                    count++
                    final url = establishUrl(datastore.baseUrl, endpoint, entity)
                    Promise<RestResponse> insertPromise = restBuilder.post(url) {
                        contentType endpoint.contentType
                        accept endpoint.acceptType ?: entity.javaClass, endpoint.accept
                        for (entry in endpoint.headers.entrySet()) {
                            header entry.key, entry.value
                        }
                        body insert
                    }.then { RestResponse response ->
                        if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                            firePostInsertEvent(entity, new BeanEntityAccess(entity, insert))
                        }

                        return response
                    }

                    insertResponses << insertPromise
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
        RestResponse response = executeGet(endpoint, datastore, pe, url, key)

        if(response.statusCode == HttpStatus.OK) {
            return bindResponseToNewEntity(response, datastore, mimeType, pe)
        }
        return null
    }

    private RestResponse executeGet(Endpoint endpoint, RestClientDatastore datastore, PersistentEntity pe, String url, Serializable identifier) {
        final sessionUrl = session.getAttribute(pe, RestClientSession.ATTRIBUTE_URL)
        final sessionRequestCustomizer = session.getAttribute(pe, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)
        url = sessionUrl ? sessionUrl.toString() : url
        Map<String, Object> args = [:]
        args.put(GrailsDomainClassProperty.IDENTITY, identifier)

        RestResponse response
        if (endpoint.async) {
            AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
            Promise<RestResponse> promise = builder.get(url, args) {
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                for (entry in endpoint.headers.entrySet()) {
                    header entry.key, entry.value
                }
                if(sessionRequestCustomizer instanceof Closure) {
                    Closure callable = (Closure)sessionRequestCustomizer
                    callable.delegate = delegate
                    callable.call()
                }
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
            response = builder.get(url, args) {
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                for (entry in endpoint.headers.entrySet()) {
                    header entry.key, entry.value
                }
                if(sessionRequestCustomizer instanceof Closure) {
                    Closure callable = (Closure)sessionRequestCustomizer
                    callable.delegate = delegate
                    callable.call()
                }
            }
        }
        response
    }

    private Object bindResponseToNewEntity(RestResponse response, RestClientDatastore datastore, MimeType mimeType, PersistentEntity pe) {
        final body = response.body
        if(body) {
            if(pe.javaClass.isInstance(body)) {
                final entityAccess = new BeanEntityAccess(pe, body)
                if( cancelLoad(pe, entityAccess)) return null
                else {
                    try {
                        return body
                    } finally {
                        firePostLoadEvent(pe, entityAccess)
                    }
                }
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
                final entityAccess = new BeanEntityAccess(pe, body)
                if( cancelLoad(pe, entityAccess)) return null
                else {
                    try {
                        return instance
                    } finally {
                        firePostLoadEvent(pe, entityAccess)
                    }
                }

            }
        }
        return null
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        Endpoint endpoint = (Endpoint)pe.mapping.mappedForm

        final Session session = getSession()
        RestClientDatastore datastore = (RestClientDatastore) session.datastore

        final identifier = getObjectIdentifier(obj)
        if(identifier) {
            // do update
            final entityAccess = new BeanEntityAccess(pe, obj)
            if( cancelUpdate(pe, entityAccess) ) return getObjectIdentifier(obj)
            if( (obj instanceof DirtyCheckable) && !session.isDirty(obj) ) {
                return getObjectIdentifier(obj)
            }

            SessionImplementor impl = (SessionImplementor) session
            if(endpoint.async) {
                impl.addPendingUpdate(new PendingUpdateAdapter<AsyncRestBuilder, Object>(pe, identifier, datastore.asyncRestClients.get(pe), new BeanEntityAccess(pe, obj)) {
                    @Override
                    void run() {
                        AsyncRestBuilder builder = getNativeEntry()
                        final sessionUrl = session.getAttribute(obj, RestClientSession.ATTRIBUTE_URL)
                        final sessionRequestCustomizer = session.getAttribute(obj, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)

                        final url = sessionUrl ? sessionUrl.toString() : establishUrl(datastore.baseUrl, endpoint, pe, identifier)
                        Map<String, Object> args = [:]
                        args.put('id', identifier)
                        Promise<RestResponse> responsePromise = builder.put(url, args) {
                            contentType endpoint.contentType
                            accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                            for (entry in endpoint.headers.entrySet()) {
                                header entry.key, entry.value
                            }
                            body obj
                            if(sessionRequestCustomizer instanceof Closure) {
                                Closure callable = (Closure)sessionRequestCustomizer
                                callable.delegate = delegate
                                callable.call()
                            }
                        }
                        RestResponse response
                        if(endpoint.readTimeout > -1)
                            response = responsePromise.get(endpoint.readTimeout, TimeUnit.SECONDS)
                        else
                            response = responsePromise.get()
                        if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                            throw new RestClientException("Status code [${response.statusCode}] returned for PUT request to URL [$url]", response)
                        }
                        else {
                            firePostUpdateEvent(pe, new BeanEntityAccess(pe, obj))
                        }
                    }
                })
            }
            else {
                RestBuilder restBuilder = datastore.syncRestClients.get(pe)
                final sessionUrl = session.getAttribute(obj, RestClientSession.ATTRIBUTE_URL)
                final sessionRequestCustomizer = session.getAttribute(obj, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)

                final url = sessionUrl ? sessionUrl.toString() : establishUrl(datastore.baseUrl, endpoint, pe, identifier)
                Map<String, Object> args = [:]
                args['id'] = identifier
                final RestResponse response = restBuilder.put(url, args) {
                    contentType endpoint.contentType
                    accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                    for (entry in endpoint.headers.entrySet()) {
                        header entry.key, entry.value
                    }
                    body obj
                    if(sessionRequestCustomizer instanceof Closure) {
                        Closure callable = (Closure)sessionRequestCustomizer
                        callable.delegate = delegate
                        callable.call()
                    }

                }
                if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                    throw new RestClientException("Status code [${response.statusCode}] returned for PUT request to URL [$url]", response)
                }
                else {
                    firePostUpdateEvent(pe, entityAccess)
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
            impl.addPendingInsert(new PendingInsertAdapter<Object, Object>(entity, identifier, datastore.asyncRestClients.get(entity), new BeanEntityAccess(entity, obj)) {

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
        final sessionUrl = session.getAttribute(obj, RestClientSession.ATTRIBUTE_URL)
        final sessionRequestCustomizer = session.getAttribute(obj, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)


        final url = sessionUrl ? sessionUrl.toString() : establishUrl(datastore.baseUrl, endpoint, pe)
        RestResponse response
        MimeType mimeType = new MimeType(endpoint.contentType)
        final entityAccess = new BeanEntityAccess(pe, obj)
        if( cancelInsert(pe, entityAccess) ) return null
        if (endpoint.async) {
            AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
            Promise<RestResponse> result = builder.post(url) {
                contentType endpoint.contentType
                accept endpoint.acceptType ?: pe.javaClass, endpoint.accept
                for (entry in endpoint.headers.entrySet()) {
                    header entry.key, entry.value
                }
                body obj
                if(sessionRequestCustomizer instanceof Closure) {
                    Closure callable = (Closure)sessionRequestCustomizer
                    callable.delegate = delegate
                    callable.call()
                }
            }.then { RestResponse r ->
                if(r.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                    firePostInsertEvent(pe, entityAccess)
                }
                return r
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
                for (entry in endpoint.headers.entrySet()) {
                    header entry.key, entry.value
                }
                body obj
                if(sessionRequestCustomizer instanceof Closure) {
                    Closure callable = (Closure)sessionRequestCustomizer
                    callable.delegate = delegate
                    callable.call()
                }
            }
            if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                firePostInsertEvent(pe, entityAccess)
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
            final entityAccess = new BeanEntityAccess(pe, object)
            if( cancelDelete(pe, entityAccess) ) return
            final id = getObjectIdentifier(object)
            if(id) {
                Endpoint endpoint = (Endpoint)pe.mapping.mappedForm
                RestClientDatastore datastore = (RestClientDatastore)getSession().datastore
                final sessionUrl = session.getAttribute(object, RestClientSession.ATTRIBUTE_URL)
                final sessionRequestCustomizer = session.getAttribute(object, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER)

                final url = sessionUrl ? sessionUrl.toString() :  establishUrl(datastore.baseUrl, endpoint, pe, id)
                if(endpoint.async) {
                    AsyncRestBuilder builder = datastore.asyncRestClients.get(pe)
                    Map<String, Object> args = [:]
                    args.put('id', id)
                    Promise<RestResponse> result = builder.delete(url, args) {
                        for (entry in endpoint.headers.entrySet()) {
                            header entry.key, entry.value
                        }
                        if(sessionRequestCustomizer instanceof Closure) {
                            Closure callable = (Closure)sessionRequestCustomizer
                            callable.delegate = delegate
                            callable.call()
                        }
                    }.then { RestResponse response ->
                        if(response.statusCode.series() == HttpStatus.Series.SUCCESSFUL) {
                            firePostDeleteEvent(pe, entityAccess)
                        }

                        return response
                    }
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
                    Map<String, Object> args = [:]
                    args.put('id', id)
                    RestResponse response = builder.delete(url, args) {
                        for (entry in endpoint.headers.entrySet()) {
                            header entry.key, entry.value
                        }
                        if(sessionRequestCustomizer instanceof Closure) {
                            Closure callable = (Closure)sessionRequestCustomizer
                            callable.delegate = delegate
                            callable.call()
                        }
                    }
                    if(response.statusCode.series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new RestClientException("Invalid status code [${response.statusCode}] returned for DELETE request to URL [$url]", response)
                    }
                    else {
                        firePostDeleteEvent(pe, entityAccess)
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
                if( cancelDelete(pe, new BeanEntityAccess(pe, obj)) ) continue
                count++
                final id = getObjectIdentifier(obj)
                if(id) {
                    final url = establishUrl(datastore.baseUrl, endpoint, pe, id)
                    Promise<RestResponse> deletePromise = restBuilder.delete(url) {
                        for (entry in endpoint.headers.entrySet()) {
                            header entry.key, entry.value
                        }
                    }
                    deletePromise.then { RestResponse response ->
                        if(response && response.statusCode.series() == HttpStatus.Series.SUCCESSFUL)
                            firePostDeleteEvent(pe, new BeanEntityAccess(pe, obj))
                    }
                    promiseList << deletePromise
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
        return new RequestParameterRestClientQuery(getSession(), getPersistentEntity())
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
        RestResponse response = executeGet(endpoint, datastore, entity, url, id)

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
