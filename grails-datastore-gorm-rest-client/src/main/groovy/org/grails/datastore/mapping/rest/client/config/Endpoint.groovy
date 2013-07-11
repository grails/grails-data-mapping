/* Copyright 2013 the original author or authors.
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
package org.grails.datastore.mapping.rest.client.config

import com.google.gson.JsonElement
import grails.plugins.rest.client.RestBuilder
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.config.Entity
import org.springframework.http.client.SimpleClientHttpRequestFactory

/**
 * Represents a configured end point for an entity mapped to a REST resource
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class Endpoint extends Entity{

    @Delegate SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory()
    /**
     * The full URL to the end point
     */
    String url
    /**
     * The URI to the endpoint. The full URL is established from the configuration
     */
    String uri
    /**
     * The content type of the end point to pass to the Content-Type header when sending requests
     */
    String contentType = MimeType.JSON.name
    /**
     * The accept type of the end point to pass to the Accept header when sending requests
     */
    String accept = contentType

    Class acceptType = JsonElement

    /**
     * Any addiitonal headers to be sent as part of the request
     */
    Map<String, String> headers = [:]
    /**
     * The timeout for establishing a connection (defaults to none)
     */
    int connectTimeout = -1
    /**
     * The timeout for reading data (defaults to none)
     */
    int readTimeout = -1

    /**
     * Whether to issue requests asynchronously and rely on timeout
     */
    boolean async = true


    Endpoint() {
        final proxy = RestBuilder.getProxyForSystemProperties()
        if(proxy) {
            httpRequestFactory.proxy = proxy
        }
    }

    /**
     * Set the underlying URLConnection's connect timeout (in milliseconds).
     * A timeout value of 0 specifies an infinite timeout.
     * <p>Default is the system's default timeout.
     * @see URLConnection#setConnectTimeout(int)
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.httpRequestFactory.setConnectTimeout(connectTimeout)
    }

    /**
     * Set the underlying URLConnection's read timeout (in milliseconds).
     * A timeout value of 0 specifies an infinite timeout.
     * <p>Default is the system's default timeout.
     * @see URLConnection#setReadTimeout(int)
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        this.httpRequestFactory.setReadTimeout(readTimeout)
    }

    int getConnectTimeout() {
        return connectTimeout
    }

    int getReadTimeout() {
        return readTimeout
    }

    String getAccept() {
        return accept
    }

    void setAccept(String accept) {
        if(accept.endsWith('/json') || accept.endsWith("+json")) {
            acceptType = JsonElement
        }
        else if(accept.endsWith('/json') || accept.endsWith("+json")) {
            acceptType = GPathResult
        }
        this.accept = accept
    }
}
