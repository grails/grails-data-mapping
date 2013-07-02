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
import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.codec.binary.Base64
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

    def body

    MultiValueMap<String, Object> mvm = new LinkedMultiValueMap<String, Object>()

    Map<String, Object> variables = [:]

    // configures basic author
    RequestCustomizer auth(String username, String password) {
        String usernameAndPassword = "$username:$password"
        String encoded = new String(Base64.encodeBase64(usernameAndPassword.bytes))
        headers.add AUTHORIZATION, "Basic $encoded".toString()
        return this
    }

    RequestCustomizer contentType(String contentType) {
        headers.setContentType(MediaType.valueOf(contentType))
        return this
    }

    RequestCustomizer accept(String... contentTypes) {
        def list = contentTypes.collect { String it -> MediaType.valueOf(it) }
        headers.setAccept(list)
        return this
    }

    RequestCustomizer header(String name, String value) {
        headers[name] = value
        return this
    }

    RequestCustomizer json(Closure callable) {
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        JSON j = new JSONBuilder().build(callable)
        json(j)
    }

    RequestCustomizer json(JSON json) {
        body = json.toString()
        if (!headers.contentType) {
            contentType "application/json"
        }
        return this
    }

    RequestCustomizer json(String json) {
        body = json
        if (!headers.contentType) {
            contentType "application/json"
        }
        return this
    }

    RequestCustomizer json(object) {
        def json = object as JSON
        body = json.toString()
        if (!headers.contentType) {
            contentType "application/json"
        }
        return this
    }

    RequestCustomizer xml(Closure closure) {
        def b = new StreamingMarkupBuilder()
        Writable markup = (Writable)b.bind(closure)
        StringWriter sw = new StringWriter()
        markup.writeTo(sw)
        body = sw.toString()
        if (!headers.contentType) {
            contentType "application/xml"
        }
        return this
    }

    RequestCustomizer xml(object) {
        def xml = object as XML
        body = xml.toString()
        if (!headers.contentType) {
            contentType "application/xml"
        }
        return this
    }

    RequestCustomizer urlVariables(Map<String, Object> variables) {
        if (variables != null) {
            this.variables = variables
        }
        return this
    }

    RequestCustomizer body(content) {
        if (content instanceof JSON) {
            if (!headers.contentType) {
                contentType "application/json"
            }
            body = content.toString()
        }
        else {
            body = content
        }

        return this
    }

    HttpEntity createEntity() {
        return mvm ? new HttpEntity(mvm, headers) : new HttpEntity(body, headers)
    }

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
