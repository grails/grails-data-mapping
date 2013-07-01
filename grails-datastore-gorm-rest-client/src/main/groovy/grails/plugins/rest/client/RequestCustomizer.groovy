package grails.plugins.rest.client

import grails.converters.JSON
import grails.converters.XML
import grails.web.JSONBuilder
import groovy.xml.StreamingMarkupBuilder

import org.codehaus.groovy.grails.plugins.codecs.Base64Codec
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

class RequestCustomizer {

    HttpHeaders headers = new HttpHeaders()

    def body

    MultiValueMap<String, Object> mvm = new LinkedMultiValueMap<String, Object>()

    Map<String, Object> variables = [:]

    // configures basic author
    RequestCustomizer auth(String username, String password) {
        String encoded = Base64Codec.encode("$username:$password")
        headers.Authorization = "Basic $encoded".toString()
        return this
    }

    RequestCustomizer contentType(String contentType) {
        headers.setContentType(MediaType.valueOf(contentType))
        return this
    }

    RequestCustomizer accept(String... contentTypes) {
        def list = contentTypes.collect { MediaType.valueOf(it) }
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
        def markup = b.bind(closure)
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
        mvm[name] = value
    }
}
