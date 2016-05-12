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
public abstract class AbstractPersistentProperty<T extends Property> implements PersistentProperty<T> {
    protected PersistentEntity owner;
    protected MappingContext context;
    protected String name;
    protected Class type;
    protected Boolean inherited;

    public AbstractPersistentProperty(PersistentEntity owner, MappingContext context, PropertyDescriptor descriptor) {
        this(owner, context, descriptor.getName(), descriptor.getPropertyType());
    }

    public AbstractPersistentProperty(PersistentEntity owner, MappingContext context, String name, Class type) {
        this.owner = owner;
        this.context = context;
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractPersistentProperty<?> that = (AbstractPersistentProperty<?>) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
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
        final T mappedForm = getMapping().getMappedForm();
        return mappedForm != null && mappedForm.isNullable();
    }

    @Override
    public boolean isInherited() {
        if(inherited == null) {

            if(owner.isRoot()) {
                inherited = false;
            }
            else {
                PersistentEntity parentEntity = owner.getParentEntity();
                boolean foundInParent = false;
                while(parentEntity != null) {
                    final PersistentProperty p = parentEntity.getPropertyByName(name);
                    if(p != null) {
                        foundInParent = true;
                        break;
                    }
                    parentEntity = parentEntity.getParentEntity();
                }

                inherited = foundInParent;
            }
        }

        return inherited;
    }
}
