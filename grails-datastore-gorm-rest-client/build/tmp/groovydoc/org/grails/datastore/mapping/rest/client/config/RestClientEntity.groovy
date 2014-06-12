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
package org.grails.datastore.mapping.rest.client.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Maps a domain class to a REST endpoint
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientEntity extends AbstractPersistentEntity<Endpoint>{
    protected RestClientClassMapping classMapping

    RestClientEntity(Class javaClass, RestClientMappingContext context) {
        super(javaClass, context)
        this.classMapping = new RestClientClassMapping(this, context)
    }


    public ClassMapping<Endpoint> getMapping() {
        this.classMapping
    }


    class RestClientClassMapping extends AbstractClassMapping<Endpoint> {
        Endpoint mappedForm
        RestClientClassMapping(PersistentEntity entity, RestClientMappingContext context) {
            super(entity, context)
            this.mappedForm = (Endpoint)getMappingContext().getMappingFactory().createMappedForm(entity)
        }
    }
}
