/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.simpledb.config;

import org.grails.datastore.mapping.model.AbstractClassMapping;
import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Models a SimpleDB-mapped entity.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBPersistentEntity extends AbstractPersistentEntity<SimpleDBDomainClassMappedForm> {

    public SimpleDBPersistentEntity(Class<?> javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<SimpleDBDomainClassMappedForm> getMapping() {
        return new SimpleDBClassMapping(this, context);
    }

    public class SimpleDBClassMapping extends AbstractClassMapping<SimpleDBDomainClassMappedForm> {

        public SimpleDBClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
        }

        @Override
        public SimpleDBDomainClassMappedForm getMappedForm() {
            return (SimpleDBDomainClassMappedForm) context.getMappingFactory().createMappedForm(SimpleDBPersistentEntity.this);
        }
    }
}
