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
package org.grails.datastore.mapping.rest.client.http.converters

import grails.rest.render.RenderContext
import grails.web.mime.MimeType
import org.grails.datastore.mapping.rest.client.config.Endpoint
import org.springframework.http.HttpMethod
import org.springframework.http.HttpOutputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import java.nio.charset.Charset

/**
 * A render context for RestTemplate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class HttpOutputMessageRenderContext implements RenderContext{

    HttpOutputMessage httpOutputMessage
    Endpoint endpoint
    HttpMethod httpMethod
    List<String> includes = null
    List<String> excludes = []
    boolean getWriterCalled = false

    HttpOutputMessageRenderContext(HttpMethod httpMethod, HttpOutputMessage httpOutputMessage, Endpoint endpoint) {
        this.httpOutputMessage = httpOutputMessage
        this.endpoint = endpoint
        this.httpMethod = httpMethod
    }

    @Override
    Map<String, Object> getArguments() {
        return Collections.emptyMap()
    }

    @Override
    String getResourcePath() {
        endpoint.url
    }

    @Override
    MimeType getAcceptMimeType() {
        final acceptList = httpOutputMessage.headers.getAccept()
        if(acceptList) {
            return new MimeType(acceptList[0].toString())
        }
        return null
    }

    @Override
    Locale getLocale() {
        return Locale.getDefault()
    }

    @Override
    Writer getWriter() {
        getWriterCalled = true
        return new OutputStreamWriter(httpOutputMessage.getBody(), getCharSetForMediaType(httpOutputMessage.headers.getContentType()))
    }

    @Override
    HttpMethod getHttpMethod() {
        return this.httpMethod
    }

    @Override
    String getActionName() { null }

    @Override
    String getControllerName() { null }

    boolean wasWrittenTo() {
        return getWriterCalled
    }

    @Override
    String getViewName() { null }

    @Override
    void setStatus(HttpStatus status) {
        // noop, can't set status for output message
    }

    @Override
    void setContentType(String contentType) {
        httpOutputMessage.headers.setContentType(MediaType.valueOf(contentType))
    }

    @Override
    void setViewName(String viewName) {
        // noop, can't set view for output message
    }

    @Override
    void setModel(Map model) {
        // noop, can't set model for output message
    }


    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }

}
