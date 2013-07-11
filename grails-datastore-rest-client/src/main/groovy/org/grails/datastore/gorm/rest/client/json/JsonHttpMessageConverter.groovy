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
package org.grails.datastore.gorm.rest.client.json

import grails.converters.JSON
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.util.StreamUtils

import java.nio.charset.Charset

/**
 * A message converter that supports JSON
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class JsonHttpMessageConverter extends AbstractHttpMessageConverter<JSONElement>{

    JsonHttpMessageConverter() {
        super(MediaType.APPLICATION_JSON)
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return JSON.isAssignableFrom(clazz) || JSONElement.isAssignableFrom(clazz)
    }

    @Override
    boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return super.canWrite(clazz, mediaType) && !JSON.isAssignableFrom(clazz)
    }

    @Override
    protected JSONElement readInternal(Class<? extends JSONElement> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final contentType = inputMessage.headers.getContentType()
        final body = inputMessage.body
        if(body) {
            return JSON.parse(body, getCharSetForMediaType(contentType).toString())
        }
        return null
    }

    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }

    @Override
    protected void writeInternal(JSONElement t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        StreamUtils.copy(toString(), getCharSetForMediaType(outputMessage.headers.getContentType()), outputMessage.body)
    }
}
