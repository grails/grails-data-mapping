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
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.binding.bindingsource.DataBindingSourceRegistry
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.rest.client.config.Endpoint
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpMethod
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException

/**
 * A message converter for converting entities
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class PersistentEntityHttpConverter<T> extends AbstractHttpMessageConverter<T>{
    PersistentEntity entity
    RendererRegistry rendererRegistry
    DataBindingSourceRegistry dataBindingSourceRegistry

    PersistentEntityHttpConverter(PersistentEntity entity, RendererRegistry rendererRegistry, DataBindingSourceRegistry dataBindingSourceRegistry) {
        super(MediaType.ALL)
        this.entity = entity
        this.rendererRegistry = rendererRegistry
        this.dataBindingSourceRegistry = dataBindingSourceRegistry
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return entity.javaClass == clazz
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final contentType = inputMessage.headers.getContentType() ?: MediaType.APPLICATION_JSON

        final bindingSource = dataBindingSourceRegistry.createDataBindingSource(new MimeType(contentType.toString()), clazz, inputMessage.body)
        if(bindingSource) {
            final instance = clazz.newInstance()
            if(bindingSource.hasIdentifier()) {
                final idValue = bindingSource.getIdentifierValue()
                final entityIdentityProperty = entity.identity
                instance[entityIdentityProperty.name] = entity.mappingContext.conversionService.convert(idValue, entityIdentityProperty.type)
            }
            DataBindingUtils.bindObjectToInstance(instance, bindingSource)
            return instance
        }
        else {
            return null
        }
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final contentType = outputMessage.headers.getContentType() ?: MediaType.APPLICATION_JSON
        final renderer = rendererRegistry.findRenderer(new MimeType(contentType.toString()), t)

        final renderContext = new HttpOutputMessageRenderContext(HttpMethod.POST, outputMessage, (Endpoint) entity.mapping.mappedForm) {
            @Override
            String getActionName() {
                return "show"
            }

            @Override
            String getControllerName() {
                return GrailsNameUtils.getPropertyName(t.getClass())
            }
        }
        renderer.render(t, renderContext)
    }
}
