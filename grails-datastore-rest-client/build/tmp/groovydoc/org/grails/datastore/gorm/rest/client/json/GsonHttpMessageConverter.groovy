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

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import grails.converters.JSON
import groovy.transform.CompileStatic
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException

import java.nio.charset.Charset

/**
 * A message converter for Google's GSON
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class GsonHttpMessageConverter extends AbstractHttpMessageConverter<JsonElement> {

    GsonHttpMessageConverter() {
        super(MediaType.APPLICATION_JSON)
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return JsonElement.isAssignableFrom(clazz)
    }

    @Override
    protected JsonElement readInternal(Class<? extends JsonElement> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final contentType = inputMessage.headers.getContentType()
        final charSet = getCharSetForMediaType(contentType)

        final inputStream = inputMessage.body
        if(inputStream) {
            return new JsonParser().parse(new JsonReader(new InputStreamReader(inputStream, charSet)))
        }
        return null
    }

    @Override
    protected void writeInternal(JsonElement t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final body = outputMessage.body
        if(body) {
            JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(body, getCharSetForMediaType(outputMessage.headers.getContentType())));
            jsonWriter.setLenient(true);
            Streams.write(t, jsonWriter);
        }
    }

    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }

}
