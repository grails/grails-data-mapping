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
import org.springframework.http.HttpInputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.util.StreamUtils

import java.nio.charset.Charset

/**
 * String converter that takes into account a null body
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class NullSafeStringHttpMessageConverter extends StringHttpMessageConverter {

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
        Charset charset = getCharSetForMediaType(inputMessage.getHeaders().getContentType());
        final body = inputMessage.getBody()
        if(body) {
            return StreamUtils.copyToString(body, charset);
        }
        return null
    }

    protected Charset getCharSetForMediaType(MediaType contentType) {
        contentType ? (contentType.charSet ? contentType.charSet : Charset.forName("UTF-8")) : Charset.forName("UTF-8")
    }

}
