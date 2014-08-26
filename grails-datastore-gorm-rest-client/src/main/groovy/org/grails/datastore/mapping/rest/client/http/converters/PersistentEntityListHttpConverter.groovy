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

import grails.rest.render.RendererRegistry
import grails.web.databinding.DataBindingUtils
import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException

/**
 * An http converter capable of returning a list of entity results
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class PersistentEntityListHttpConverter<T> extends AbstractHttpMessageConverter<List<T>>{
    PersistentEntity entity
    RendererRegistry rendererRegistry
    DataBindingSourceRegistry dataBindingSourceRegistry

    PersistentEntityListHttpConverter(PersistentEntity entity, RendererRegistry rendererRegistry, DataBindingSourceRegistry dataBindingSourceRegistry) {
        super(MediaType.ALL)
        this.entity = entity
        this.rendererRegistry = rendererRegistry
        this.dataBindingSourceRegistry = dataBindingSourceRegistry
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz == List
    }

    @Override
    boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected List<T> readInternal(Class<? extends List<T>> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final contentType = inputMessage.headers.getContentType() ?: MediaType.APPLICATION_JSON

        final bindingSource = dataBindingSourceRegistry.createCollectionDataBindingSource(new MimeType(contentType.toString()), entity.javaClass, inputMessage.body)
        if(bindingSource) {
            List results = []
            DataBindingUtils.bindToCollection(entity.javaClass, results, bindingSource)
            return results
        }
        else {
            return []
        }
    }

    @Override
    protected void writeInternal(List<T> t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Writing of list [$t] not supported")
    }
}

