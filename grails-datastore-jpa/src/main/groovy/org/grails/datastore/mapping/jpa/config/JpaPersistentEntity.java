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
package org.grails.datastore.mapping.jpa.config;

import javax.persistence.Table;

import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Models a JPA-mapped entity.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaPersistentEntity extends AbstractPersistentEntity<Table> {

    public JpaPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @Override
    public ClassMapping<Table> getMapping() {
        return new ClassMapping<Table>() {
            public PersistentEntity getEntity() {
                return JpaPersistentEntity.this;
            }

            @SuppressWarnings("unchecked")
            public Table getMappedForm() {
                return (Table) getJavaClass().getAnnotation(Table.class);
            }

            public IdentityMapping getIdentifier() {
                return null;
            }
        };
    }
}
