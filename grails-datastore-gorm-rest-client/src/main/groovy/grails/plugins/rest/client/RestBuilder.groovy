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
package grails.plugins.rest.client

import groovy.transform.CompileStatic
import org.springframework.http.HttpMethod

import static org.springframework.http.HttpMethod.*
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


/**
 * Main API entry to the synchronous version of the REST low-level client API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestBuilder {

    RestTemplate restTemplate = new RestTemplate()

    RestBuilder() {}

    RestBuilder(Map settings) {

        def proxyHost = System.getProperty("http.proxyHost")
        def proxyPort = System.getProperty("http.proxyPort")

        if (proxyHost && proxyPort) {
            if (settings.proxy == null) {
                settings.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort.toInteger()))
            }
        }

        if (settings.proxy instanceof Map) {
            def ps = ((Map)settings.proxy).entrySet().iterator().next()
            if (ps.value) {
                def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ps.key.toString(), ps.value.toString().toInteger()))
                settings.proxy = proxy
            }
        }

        final customRequestFactory = new SimpleClientHttpRequestFactory()
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(SimpleClientHttpRequestFactory)
        for(key in settings.keySet()) {
            final prop = key.toString()
            if(customRequestFactory.hasProperty(prop)) {
                metaClass.setProperty(customRequestFactory, prop, settings.get(key))
            }
        }
        restTemplate.setRequestFactory(customRequestFactory)
    }

    RestBuilder(RestTemplate restTemplate) {
        this.restTemplate = restTemplate
    }
    /**
     * Issues a GET request and returns the response in the most appropriate type
     * @param url The URL
     * @param url The closure customizer used to customize request attributes
     */
    RestResponse get(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, GET
    }

    /**
     * Issues a GET request and returns the response in the most appropriate type
     * @param url The URL
     * @paral urlVariables The variables required by the URL pattern
     * @param url The closure customizer used to customize request attributes
     */
    RestResponse get(String url, Map<String, Object> urlVariables, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, GET, urlVariables
    }


    /**
     * Issues a PUT request and returns the response in the most appropriate type
     *
     * @param url The URL
     * @param customizer The clouser customizer
     */
    RestResponse put(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, PUT
    }

    /**
     * Issues a PUT request and returns the response in the most appropriate type
     *
     * @param url The URL
     * @param customizer The clouser customizer
     */
    RestResponse put(String url, Map<String, Object> urlVariables, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, PUT, urlVariables
    }

    /**
     * Issues a POST request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */

    RestResponse post(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, POST
    }

    /**
     * Issues a POST request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */

    RestResponse post(String url, Map<String, Object> urlVariables, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, POST, urlVariables
    }

    /**
     * Issues a PATCH request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */

    RestResponse patch(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, PATCH
    }

    /**
     * Issues a PATCH request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */

    RestResponse patch(String url, Map<String, Object> urlVariables, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, PATCH, urlVariables
    }

    /**
     * Issues DELETE a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse delete(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, DELETE
    }

    /**
     * Issues DELETE a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse delete(String url, Map<String, Object> urlVariables,@DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, DELETE, urlVariables
    }


    /**
     * Issues HEAD a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse head(String url, Map<String, Object> urlVariables,@DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, HEAD, urlVariables
    }

    /**
     * Issues HEAD a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse head(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, HEAD
    }

    /**
     * Issues OPTIONS a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse options(String url, Map<String, Object> urlVariables,@DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, OPTIONS, urlVariables
    }

    /**
     * Issues OPTIONS a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse options(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, OPTIONS
    }

    /**
     * Issues TRACE a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse trace(String url, Map<String, Object> urlVariables,@DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, TRACE, urlVariables
    }

    /**
     * Issues TRACE a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    RestResponse trace(String url, @DelegatesTo(RequestCustomizer) Closure customizer = null) {
        doRequestInternal url, customizer, TRACE
    }

    protected RestResponse doRequestInternal(String url, Closure customizer, HttpMethod method, Map<String, Object> urlVariables = Collections.emptyMap()) {

        def requestCustomizer = new RequestCustomizer()
        requestCustomizer.variables.putAll(urlVariables)
        if (customizer != null) {
            customizer.delegate = requestCustomizer
            customizer.call()
        }

        try {
            def responseEntity = restTemplate.exchange(url, method, requestCustomizer.createEntity(),
                    String, requestCustomizer.getVariables())
            handleResponse(responseEntity)
        }
        catch (HttpStatusCodeException e) {
            return new RestResponse(new ResponseEntity(e.getResponseBodyAsByteArray(), e.responseHeaders, e.statusCode))
        }
    }

    protected RestResponse handleResponse(ResponseEntity responseEntity) {
        return new RestResponse(responseEntity)
    }
}
