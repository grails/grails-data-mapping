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
package org.grails.datastore.gorm.rest.client.utils

import groovy.transform.CompileStatic
import org.grails.web.converters.Converter
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException

import java.nio.charset.Charset

/**
 * A message converter that is capable of using Grails' converters API to write messages
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class GrailsConverterHttpMessageConverter extends AbstractHttpMessageConverter<Converter>{

    GrailsConverterHttpMessageConverter() {
        super(MediaType.ALL)
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Converter.isAssignableFrom(clazz)
    }

    @Override
    boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected Converter readInternal(Class<? extends Converter> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading not supported by GrailsConverterHttpMessageConverter")
    }

    @Override
    protected void writeInternal(Converter t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        t.render(new OutputStreamWriter(outputMessage.body, getCharSetForMediaType(outputMessage.headers.getContentType())))
    }

    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }
}
