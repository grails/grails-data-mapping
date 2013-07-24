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
package org.grails.datastore.mapping.rest.client.query

import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.core.Session
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.rest.client.engine.RestClientEntityPersister
import org.grails.datastore.mapping.rest.client.config.Endpoint
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.grails.datastore.mapping.rest.client.util.UrlBuilder
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.plugins.rest.client.RestResponse
import grails.async.Promise
import java.util.concurrent.TimeUnit
import grails.plugins.rest.client.RestBuilder
import static org.grails.datastore.mapping.query.Query.*

/**
 * A very simple implementation of the {@link Query} abstract class that uses query parameters to the REST endpoint to execute a query.
 *
 * The implementation only supports {@link org.grails.datastore.mapping.query.Query.Conjunction} and {@link org.grails.datastore.mapping.query.Query.Equals} criterion. All other
 * criterion simply throw @{@link UnsupportedOperationException}
 *
 * Queries are generated in the form http://myhost/endpoint?foo=bar&bar=foo&max=10&offset=0
 *
 * The server is required to handle the parameters as appropriate.
 */
@CompileStatic
class RequestParameterRestClientQuery extends Query {

    RequestParameterRestClientQuery(Session session, PersistentEntity entity) {
        super(session, entity)
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        final datastore = (RestClientDatastore) session.datastore
        final entityPersister = (RestClientEntityPersister)session.getPersister(entity)
        Endpoint endpoint = (Endpoint)entity.mapping.mappedForm

        UrlBuilder urlBuilder = new UrlBuilder(entityPersister.establishUrl(datastore.baseUrl,endpoint, entity))
        if(offset > 0) {
            urlBuilder.addParam("offset", offset)
        }
        if(max > -1) {
            urlBuilder.addParam("max", max)
        }

        if(criteria instanceof Query.Conjunction) {
            Query.Conjunction conjunction = (Query.Conjunction)criteria

            for(Query.Criterion c in conjunction.getCriteria()) {
                if(c instanceof Equals) {
                    Equals equals = (Equals) c
                    urlBuilder.addParam(equals.getProperty(), equals.value)
                }
                else {
                    throw new UnsupportedOperationException("Only equals queries are supported by implementation")
                }
            }


            RestResponse response
            if(endpoint.async) {
                AsyncRestBuilder builder = datastore.asyncRestClients.get(entity)
                Promise<RestResponse> promise = builder.get(urlBuilder.toString()) {
                    accept List, endpoint.accept
                }
                if (endpoint.readTimeout > -1) {
                    response = promise.get(endpoint.readTimeout, TimeUnit.SECONDS)
                }
                else {
                    response = promise.get()
                }
            }
            else {
                RestBuilder builder = datastore.syncRestClients.get(entity)
                response = builder.get(urlBuilder.toString()) {
                    accept List, endpoint.accept
                }
            }

            return (List)response.getBody()
        }
        else {
            throw new UnsupportedOperationException("Only conjunctions are supported by query implementation")
        }
    }
}
