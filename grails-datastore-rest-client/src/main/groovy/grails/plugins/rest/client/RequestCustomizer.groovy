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

import grails.converters.JSON
import grails.converters.XML
import grails.web.JSONBuilder
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.codec.binary.Base64
import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

import static org.codehaus.groovy.grails.web.servlet.HttpHeaders.*

/**
 * Core of the DSL for building REST requests
 *
 * @since 1.0
 * @author Graeme Rocher
 */
@CompileStatic
class RequestCustomizer {

    HttpHeaders headers = new HttpHeaders()

    Class acceptType = String
    def body

    MultiValueMap<String, Object> mvm = new LinkedMultiValueMap<String, Object>()

    Map<String, Object> urlVariables = [:]

    /**
     * Used to configure BASIC authentication. Example:
     *
     * <pre><code>
     * builder.put("http://..") {
     *      auth "myuser", "mypassword"
     * }
     * </code></pre>
     *
     * @param username The username
     * @param password The password
     * @return The customizer
     */
    RequestCustomizer auth(String username, String password) {
        String usernameAndPassword = "$username:$password"
        String encoded = new String(Base64.encodeBase64(usernameAndPassword.bytes))
        headers.add AUTHORIZATION, "Basic $encoded".toString()
        return this
    }

    /**
     * Sets the Authorization HTTP header to the given value. Used typically to pass OAuth access tokens.
     *
     * <pre><code>
     * builder.put("http://..") {
     *      auth myToken
     * }
     * </code></pre>
     *
     * @param accessToken The access token
     * @return The customizer
     */
    RequestCustomizer auth(String accessToken) {
        headers.add AUTHORIZATION, accessToken
        return this
    }

    /**
     * Sets the Content-Type HTTP header to the given value. Example:
     *
     * <pre><code>
     * restBuilder.put("http://..") {
     *      contentType "application/xml"
     * }
     * </code></pre>
     *
     * @param contentType The content type
     * @return The customizer
     */
    RequestCustomizer contentType(String contentType) {
        headers.setContentType(MediaType.valueOf(contentType))
        return this
    }

    /**
     * Sets the Accept HTTP header to the given value. Example:
     *
     * <pre><code>
     * restBuilder.get("http://..") {
     *      accept "application/xml"
     * }
     * </code></pre>
     *
     * @param contentTypes The content types
     * @return The customizer
     */
    RequestCustomizer accept(String... contentTypes) {
        def list = contentTypes.collect { String it -> MediaType.valueOf(it) }
        headers.setAccept(list)
        return this
    }

    /**
     * Sets the Accept HTTP header to the given value. Example:
     *
     * <pre><code>
     * restBuilder.get("http://..") {
     *      accept XML
     * }
     * </code></pre>
     *
     * @param responseType The expected response type
     * @param contentTypes The content types
     * @return The customizer
     */
    RequestCustomizer accept(Class responseType, String... contentTypes) {
        if(responseType == JSON) {
            if(!contentTypes)
                accept(MediaType.APPLICATION_JSON_VALUE)
            acceptType = JSON
        }
        else if (responseType == XML) {
            if(!contentTypes)
                accept(MediaType.APPLICATION_XML_VALUE)
            acceptType = XML

        }
        else {
            acceptType = responseType
        }
        if(contentTypes) {
            accept(contentTypes)
        }
        return this
    }

    /**
     * Sets an HTTP header to the given name and value. Example:
     *
     * <pre><code>
     * restBuilder.get("http://..") {
     *      header "Accept", "text/xml"
     * }
     * </code></pre>
     *
     * @param name The name of the header
     * @param value The value of the header
     * @return The customizer
     */
    RequestCustomizer header(String name, String value) {
        headers[name] = value
        return this
    }

    /**
     * Sets the body of the request to the JSON defined by the closure. The closure uses JSONBuilder to product a JSON object. Example:
     *
     * <pre><code>
     * restBuilder.put("http://..") {
     *     json {
     *        name = "test-group"
     *        description = "A temporary test group"
     *     }
     * }
     * </code></pre>
     *
     * @param callable The callable closure
     * @return This customizer
     */
    RequestCustomizer json(@DelegatesTo(JSONBuilder) Closure callable) {
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        JSON j = new JSONBuilder().build(callable)
        json(j)
    }

    /**
     * Sets the body of the request to the passed JSON
     *
     * @param json The JSON object
     * @return this customizer
     */
    RequestCustomizer json(JSON json) {
        body = json
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_JSON_VALUE
        }
        return this
    }

    /**
     * Sets the body of the request to the passed JSON
     *
     * @param json The JSON object
     * @return this customizer
     */
    RequestCustomizer json(JSONElement json) {
        body = json
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_JSON_VALUE
        }
        return this
    }

    /**
     * Sets the body of the request to the passed JSON string
     *
     * @param json The JSON object
     * @return this customizer
     */
    RequestCustomizer json(String json) {
        body = json
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_JSON_VALUE
        }
        return this
    }

    /**
     * Converts the given object to JSON and sets the body to the converted JSON object
     *
     * @param object The object to convert to JSON
     * @return this customizer
     */
    RequestCustomizer json(object) {
        json(object as JSON)
    }

    /**
     * Sets the body of the request to the XML defined by the closure. Uses {@link StreamingMarkupBuilder} to produce the XML
     *
     * @param closure The closure that defines the XML
     * @return This customizer
     */
    RequestCustomizer xml(@DelegatesTo(StreamingMarkupBuilder)Closure closure) {
        def b = new StreamingMarkupBuilder()
        Writable markup = (Writable)b.bind(closure)
        body = markup
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_XML_VALUE
        }
        return this
    }

    /**
     * Sets the body of the request to the XML converter argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    RequestCustomizer xml(XML xml) {
        body = xml
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_XML_VALUE
        }
        return this
    }

    /**
     * Sets the body of the request to the XML string argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    RequestCustomizer xml(String xml) {
        body = xml
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_XML_VALUE
        }
        return this
    }

    /**
     * Sets the body of the request to the XML GPathResult argument.
     *
     * @param xml The XML to be used as the body of the request
     * @return This customizer
     */
    RequestCustomizer xml(GPathResult xml) {
        body = xml
        if (!headers.contentType) {
            contentType MediaType.APPLICATION_XML_VALUE
        }
        return this
    }

    /**
     * Converts the given object to XML using Grails' converters and sets the XML as the body of the request
     *
     * @param object The object
     * @return This customizer
     */
    RequestCustomizer xml(object) {
        xml(object as XML)
    }

    /**
     * Configures any variables used in the case of a templated URL. Example:
     *
     * <pre><code>
     * restBuilder.get("http://../book/{id}") {
     *      urlVariables id:1
     * }
     * </code></pre>
     *
     * @param variables The URL variables
     * @return
     */
    RequestCustomizer urlVariables(Map<String, Object> variables) {
        if (variables != null) {
            this.urlVariables = variables
        }
        return this
    }

    /**
     * Sets the body of the request to the given argument. Note that if you argument is not convertable to a message body an exception will be thrown.
     * You can register new converters using restBuilder.restTemplate.getMessageConverters().add(..)
     *
     * @param content The body content
     * @return This customizer
     */
    RequestCustomizer body(content) {
        if ( (content instanceof JSON) || (content instanceof JSONElement)) {
            if (!headers.contentType) {
                contentType MediaType.APPLICATION_JSON_VALUE
            }
            body = content
        }
        else if( (content instanceof XML) || (content instanceof GPathResult)) {
            if (!headers.contentType) {
                contentType MediaType.APPLICATION_XML_VALUE
            }
            body = content
        }
        else {
            body = content
        }

        return this
    }

    HttpEntity createEntity() {
        return mvm ? new HttpEntity(mvm, headers) : new HttpEntity(body, headers)
    }

    /**
     * Sets multipart values within the request body
     *
     * @param name The name of the multipart
     * @param value The value of the multipart
     */
    void setProperty(String name, value) {
        if (value instanceof File) {
            value = new FileSystemResource(value)
        }
        else if (value instanceof URL) {
            value = new UrlResource(value)
        }
        else if (value instanceof InputStream) {
            value = new InputStreamResource(value)
        }
        mvm[name].add value
    }
}
