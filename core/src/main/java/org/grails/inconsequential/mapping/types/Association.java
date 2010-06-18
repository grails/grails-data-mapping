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
package org.grails.inconsequential.mapping.types;

import org.grails.inconsequential.mapping.*;

import java.beans.PropertyDescriptor;

/**
 * Models an association between one class and another
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class Association<T> extends AbstractPersistentProperty {
    private PersistentEntity associatedEntity;
    private String referencedPropertyName;
    private boolean owningSide;

    public Association(PersistentEntity owner, MappingContext context, PropertyDescriptor descriptor) {
        super(owner, context, descriptor);
    }

    public Association(PersistentEntity owner, MappingContext context, String name, Class type) {
        super(owner, context, name, type);
    }

    public boolean isBidirectional() {
        return getInverseSide() != null;
    }

    public Association getInverseSide() {
        final PersistentProperty associatedProperty = associatedEntity.getPropertyByName(referencedPropertyName);
        if(associatedProperty == null) return null;
        if(associatedProperty instanceof Association) {
            return (Association) associatedProperty;
        }
        else {
            throw new IllegalMappingException("The inverse side ["+associatedEntity.getName()+"." + associatedProperty.getName() +"] of the association ["+getOwner().getName()+"." + this.getName() +"] is not valid. Associations can only map to other entities and collection types.");
        }

    }

    /**
     * Returns whether this side owns the relationship. This controls
     * the default cascading behavior if none is specified
     *
     * @return True if this property is the owning side
     */
    public boolean isOwningSide() {
        return owningSide;
    }

    public void setOwningSide(boolean owningSide) {
        this.owningSide = owningSide;
    }

    public void setAssociatedEntity(PersistentEntity associatedEntity) {
        this.associatedEntity = associatedEntity;
    }

    public PersistentEntity getAssociatedEntity() {
        return associatedEntity;
    }

    public void setReferencedPropertyName(String referencedPropertyName) {
        this.referencedPropertyName = referencedPropertyName;
    }

    public String getReferencedPropertyName() {
        return referencedPropertyName;
    }
}
