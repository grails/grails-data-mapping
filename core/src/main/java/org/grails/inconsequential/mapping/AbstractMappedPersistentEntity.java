/* Copyright 2004-2005 the original author or authors.
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
package org.grails.inconsequential.mapping;

/**
 * Abstract implementation that provides a ClassMapping instance that can be
 * used to establish further info about how the entity maps to the underlying
 * datastore
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractMappedPersistentEntity<T> extends AbstractPersistentEntity implements MappedPersistentEntity {
    public AbstractMappedPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @Override
    public void initialize() {
        this.identity = context.getMappingSyntaxStrategy().getIdentity(javaClass, context);
        this.persistentProperties = context.getMappingSyntaxStrategy().getPersistentProperties(javaClass, context, getMapping());
        if(persistentProperties != null) {
            for (PersistentProperty persistentProperty : persistentProperties) {
                propertiesByName.put(persistentProperty.getName(), persistentProperty);
            }
        }
    }

    public ClassMapping<T> getMapping() {
        return new AbstractClassMapping<T>(this, context) {
            public T getMappedForm() {
                return null;
            }
        };
    }
}
