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

import groovy.lang.Closure;
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

import java.util.List;

/**
 * Models a JPA-mapped entity.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaPersistentEntity extends AbstractPersistentEntity<org.grails.datastore.mapping.jpa.config.Table> {

    private final org.grails.datastore.mapping.jpa.config.Table mappedForm;

    public JpaPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass, MappingContext context) {
        super(javaClass, context);
        Table ann = (Table) getJavaClass().getAnnotation(Table.class);
        this.mappedForm = new org.grails.datastore.mapping.jpa.config.Table(ann);
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(getJavaClass());
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(mappedForm, org.grails.datastore.mapping.jpa.config.Table.class);

        List<Closure> values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.MAPPING, Closure.class);
        for (int i = values.size(); i > 0; i--) {
            Closure value = values.get(i - 1);
            builder.evaluate(value);
        }
        values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.CONSTRAINTS, Closure.class);
        for (int i = values.size(); i > 0; i--) {
            Closure value = values.get(i - 1);
            builder.evaluate(value);
        }

    }

    @Override
    public ClassMapping<org.grails.datastore.mapping.jpa.config.Table> getMapping() {

        return new ClassMapping<org.grails.datastore.mapping.jpa.config.Table>() {
            public PersistentEntity getEntity() {
                return JpaPersistentEntity.this;
            }

            @SuppressWarnings("unchecked")
            public org.grails.datastore.mapping.jpa.config.Table getMappedForm() {
                return mappedForm;
            }

            public IdentityMapping getIdentifier() {
                return null;
            }
        };
    }
}
