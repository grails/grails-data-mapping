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

package org.grails.datastore.gorm.rest.client

import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.core.Datastore
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersApi
import org.grails.datastore.mapping.rest.client.RestClientSession

/**
 * Extensions to the instance API for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RestClientGormInstanceApi<D> extends GormInstanceApi<D> {

    RestClientGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Converter an instance from one format to another
     *
     * @param instance The instance
     * @param clazz The type to convert to
     * @return the converted object
     */
    public Object asType(Object instance, Class<?> clazz) {
        new ConvertersApi().asType(instance, clazz)
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public Object put(Object instance, URL url = null) {
        if (url) {
            datastore.currentSession.setAttribute(instance, "url", url)
        }
        try {
            return save(instance)
        } finally {
            if (url) {
                datastore.currentSession.setAttribute(instance, "url", null)
            }
        }
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public Object post(Object instance, URL url = null) {
        if (url) {
            datastore.currentSession.setAttribute(instance, "url", url)
        }
        try {
            return insert(instance)
        } finally {
            if (url) {
                datastore.currentSession.setAttribute(instance, "url", null)
            }
        }
    }
}
