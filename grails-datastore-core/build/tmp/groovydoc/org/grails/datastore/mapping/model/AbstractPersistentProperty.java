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
package org.grails.datastore.mapping.model;

import java.beans.PropertyDescriptor;

import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.reflect.NameUtils;

/**
 * Abstract implementation of the PersistentProperty interface that uses the
 * PropertyDescriptor instance to establish name and type.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractPersistentProperty implements PersistentProperty {
    protected PersistentEntity owner;
    protected MappingContext context;
    protected String name;
    protected Class type;

    public AbstractPersistentProperty(PersistentEntity owner, MappingContext context, PropertyDescriptor descriptor) {
        this.owner = owner;
        this.context = context;
        this.name = descriptor.getName();
        this.type = descriptor.getPropertyType();
    }

    public AbstractPersistentProperty(PersistentEntity owner, MappingContext context, String name, Class type) {
        this.owner = owner;
        this.context = context;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getCapitilizedName() {
        return NameUtils.capitalize(getName());
    }

    public Class getType() {
        return type;
    }

    public PersistentEntity getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return getName() + ":" + getType().getName();
    }

    public boolean isNullable() {
        final Object mappedForm = getMapping().getMappedForm();
        return mappedForm instanceof Property && ((Property)mappedForm).isNullable();
    }
}
