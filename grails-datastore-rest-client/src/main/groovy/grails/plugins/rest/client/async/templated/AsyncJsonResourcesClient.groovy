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
package grails.plugins.rest.client.async.templated

import grails.converters.JSON
import grails.plugins.rest.client.async.AbstractAsyncResourcesClient
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.plugins.rest.client.templated.JsonResourcesClient
import groovy.transform.CompileStatic
import org.grails.web.json.JSONElement
import org.springframework.http.MediaType

/**
 * Asynchronous version of the {@link JsonResourcesClient} class
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class AsyncJsonResourcesClient extends AbstractAsyncResourcesClient<JSONElement>{

    /**
     * @param url The base URL. Example http://localhost:8080/books
     */
    AsyncJsonResourcesClient(String url) {
        super(url)
    }

    AsyncJsonResourcesClient(String url, AsyncRestBuilder restBuilder) {
        super(url, restBuilder)
    }

    @Override
    Class getAcceptType() { JSON }

    @Override
    String getAcceptContentType() { MediaType.APPLICATION_JSON_VALUE }
}
