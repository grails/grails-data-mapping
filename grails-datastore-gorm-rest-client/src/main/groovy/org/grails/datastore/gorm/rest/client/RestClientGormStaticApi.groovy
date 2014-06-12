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
package org.grails.datastore.gorm.rest.client

import grails.plugins.rest.client.async.AsyncRestBuilder
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.core.Session
import grails.plugins.rest.client.RequestCustomizer
import org.grails.datastore.mapping.rest.client.RestClientSession
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Extensions to the static API for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientGormStaticApi<D> extends GormStaticApi<D> {
    RestClientGormStaticApi(Class<D> persistentClass, RestClientDatastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders)
    }

    RestClientGormStaticApi(Class<D> persistentClass, RestClientDatastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    /**
     * Obtains the RestBuilder instance used by this domain
     */
    AsyncRestBuilder getRestBuilder() {
        ((RestClientDatastore)datastore).asyncRestClients.get(persistentEntity)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    List<D> getAll(URL url, Serializable... ids) {
        execute({ Session session ->
            withCustomUrl(session, url) {
                getAll(ids)
            }
        } as SessionCallback)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    D get(URL url, Serializable id) {
        execute({ Session session ->
            withCustomUrl(session, url) {
                get(id)
            }
        } as SessionCallback)

    }

    @CompileStatic(TypeCheckingMode.SKIP)
    List<D> getAll(URL url, Iterable<Serializable> ids, @DelegatesTo(RequestCustomizer) Closure customizer) {
        execute({ Session session ->
            withRequestCustomizer(session, customizer) {
                withCustomUrl(session, url) {
                    getAll(ids.toList().toArray())
                }
            }
        } as SessionCallback)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    D get(URL url, Serializable id, @DelegatesTo(RequestCustomizer) Closure customizer) {
        execute({ Session session ->
            withRequestCustomizer(session, customizer) {
                withCustomUrl(session, url) {
                    get(id)
                }
            }
        } as SessionCallback)

    }


    protected Object withCustomUrl(Session currentSession, URL url, Closure callable) {
        if (url) {
            currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, url)
        }
        try {
            return callable.call()
        } finally {
            if (url) {
                currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }

    protected Object withRequestCustomizer(Session currentSession, @DelegatesTo(RequestCustomizer) Closure customizer, Closure callable) {
        if (customizer) {
            currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER, customizer)
        }
        try {
            callable.call()
        } finally {
            if (customizer) {
                currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }
}
