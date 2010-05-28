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

import org.grails.inconsequential.mapping.types.Identity;
import org.grails.inconsequential.mapping.types.Simple;

import java.beans.PropertyDescriptor;

/**
 * <p>An abstract factory for creating persistent property instances.</p>
 *
 * <p>Subclasses should implement the createMappedForm method in order to
 * provide a mechanisms for representing the property in a form appropriate
 * for mapping to the underlying datastore. Example:</p>
 *
 * <pre>
 *  <code>
 *      class RelationalPropertyFactory<Column> extends PropertyFactory {
 *            public Column createMappedForm(PersistentProperty mpp) {
 *                return new Column(mpp)
 *            }
 *      }
 *  </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class MappingFactory<R,T> {

    /**
     * Creates the mapped form of a persistent entity
     *
     * @param entity The entity
     * @return The mapped form
     */
    public abstract R createMappedForm(PersistentEntity entity);
    /**
     * Creates the mapped form of a PersistentProperty instance
     * @param mpp The PersistentProperty instance
     * @return The mapped form
     */
    public abstract T createMappedForm(PersistentProperty mpp);

    /**
     * Creates an identifier property
     *
     * @param owner The owner
     * @param context The context
     * @param pd The PropertyDescriptor
     * @return An Identity instance
     */
    public Identity<T> createIdentity(PersistentEntity owner, MappingContext context, PropertyDescriptor pd) {
        return new Identity<T>(owner, context, pd) {
            @Override
            public PropertyMapping<T> getMapping() {
                if(owner instanceof MappedPersistentEntity) {
                    return createPropertyMapping(this, owner);
                }
                return null;
            }
        };
    }

    /**
     * Creates a simple property type used for mapping basic types such as String, long, integer etc.
     *
     * @param owner The owner
     * @param context The MappingContext
     * @param pd The PropertyDescriptor
     * @return A Simple property type
     */
    public Simple<T> createSimple(PersistentEntity owner, MappingContext context, PropertyDescriptor pd) {
        return new Simple<T>(owner, context, pd) {
            @Override
            public PropertyMapping<T> getMapping() {
                if(owner instanceof MappedPersistentEntity) {
                    return createPropertyMapping(this, owner);
                }
                return null;
            }
        };
    }

    protected PropertyMapping<T> createPropertyMapping(final MappedPersistentProperty<T> property, final PersistentEntity owner) {
        return new PropertyMapping<T>() {
            public ClassMapping getClassMapping() {
                return ((MappedPersistentEntity) owner).getMapping();
            }
            public T getMappedForm() {
                return createMappedForm(property);
            }
        };
    }

}

