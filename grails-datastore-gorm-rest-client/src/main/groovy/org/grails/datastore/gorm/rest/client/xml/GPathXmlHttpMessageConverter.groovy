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
package org.grails.datastore.gorm.rest.client.xml

import grails.converters.XML
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException

import java.nio.charset.Charset

/**
 * A {@link org.springframework.http.converter.HttpMessageConverter} for converting responses to a GPathResult
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class GPathXmlHttpMessageConverter extends AbstractHttpMessageConverter<groovy.util.slurpersupport.GPathResult>{
    @Override
    protected boolean supports(Class<?> clazz) {
        return XML
    }

    @Override
    protected GPathResult readInternal(Class<? extends GPathResult> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return (GPathResult)XML.parse(inputMessage.body, getCharSetForMediaType(inputMessage.headers.getContentType()).toString())
    }

    @Override
    protected void writeInternal(GPathResult t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Writable w = getWritablePathResult(t)
        w.writeTo(new OutputStreamWriter(outputMessage.body, getCharSetForMediaType(outputMessage.headers.getContentType())))
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Writable getWritablePathResult(GPathResult t) {
        (Writable) new StreamingMarkupBuilder().bind {
            out << t
        }
    }

    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }
}
