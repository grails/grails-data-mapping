/*
 * Copyright 2015 original authors
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
package org.grails.datastore.mapping.model.types;

import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;

import jakarta.persistence.FetchType;
import java.beans.PropertyDescriptor;

/**
 * Shared super class inherited by both {@link OneToMany} and {@link ManyToMany}
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public abstract class ToMany<T extends Property> extends Association<T> {


    public ToMany(PersistentEntity owner, MappingContext context, PropertyDescriptor descriptor) {
        super(owner, context, descriptor);
    }

    public ToMany(PersistentEntity owner, MappingContext context, String name, @SuppressWarnings("rawtypes") Class type) {
        super(owner, context, name, type);
    }

    /**
     * @return Whether this association is lazy
     */
    public boolean isLazy() {
        return getFetchStrategy() == FetchType.EAGER && getMapping().getMappedForm().isLazy();
    }

}

