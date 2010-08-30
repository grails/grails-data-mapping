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
package org.springframework.datastore.mapping;

import org.springframework.datastore.core.EntityCreationException;
import org.springframework.datastore.mapping.lifecycle.Initializable;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.mapping.types.OneToMany;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Abstract implementation to be subclasses on a per datastore basis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractPersistentEntity<T> implements PersistentEntity, Initializable{
    protected Class javaClass;
    protected List<PersistentProperty> persistentProperties;
    protected List<Association> associations;
    protected Map<String, PersistentProperty> propertiesByName = new HashMap<String,PersistentProperty>();
    protected MappingContext context;
    protected PersistentProperty identity;
    protected List<String> persistentPropertyNames;
    private String decapitalizedName;
    protected Set owners;
    private PersistentEntity parentEntity = null;

    public AbstractPersistentEntity(Class javaClass, MappingContext context) {
        if(javaClass == null) throw new IllegalArgumentException("The argument [javaClass] cannot be null");
        this.javaClass = javaClass;
        this.context = context;
        this.decapitalizedName = Introspector.decapitalize(javaClass.getName());
    }

    public void initialize() {
        this.identity = context.getMappingSyntaxStrategy().getIdentity(javaClass, context);
        this.owners = context.getMappingSyntaxStrategy().getOwningEntities(javaClass, context);
        this.persistentProperties = context.getMappingSyntaxStrategy().getPersistentProperties(javaClass, context);
        persistentPropertyNames = new ArrayList<String>();
        associations = new ArrayList();
        for (PersistentProperty persistentProperty : persistentProperties) {
            if(!(persistentProperty instanceof OneToMany))
                persistentPropertyNames.add(persistentProperty.getName());
            if(persistentProperty instanceof Association) {
                associations.add((Association) persistentProperty);
            }
        }
        for (PersistentProperty persistentProperty : persistentProperties) {
            propertiesByName.put(persistentProperty.getName(), persistentProperty);
        }

        Class superClass = javaClass.getSuperclass();
        if (!javaClass.getSuperclass().equals(Object.class) &&
            !Modifier.isAbstract(javaClass.getSuperclass().getModifiers())) {
            this.parentEntity = context.addPersistentEntity(superClass);
        }




        getMapping().getMappedForm(); // initialize mapping
    }

    public PersistentEntity getParentEntity() {
        return parentEntity;
    }

    public String getDiscriminator() {
        return getJavaClass().getSimpleName();
    }

    public PersistentEntity getRootEntity() {
        PersistentEntity root = this;
        PersistentEntity parent = getParentEntity();
        while(parent != null) {
            root = parent;
            parent = parent.getParentEntity();
        }
        return root;
    }

    public boolean isRoot() {
        return getParentEntity() == null;
    }

    public boolean isOwningEntity(PersistentEntity owner) {
        return owner != null && owners.contains(owner.getJavaClass());
    }

    public String getDecapitalizedName() {
        return this.decapitalizedName;
    }

    public List<String> getPersistentPropertyNames() {
        return this.persistentPropertyNames;
    }

    public ClassMapping<T> getMapping() {
        return new AbstractClassMapping<T>(this, context) {
            public T getMappedForm() {
                return null;
            }
        };
    }

    public Object newInstance() {
        try {
            return getJavaClass().newInstance();
        } catch (InstantiationException e) {
            throw new EntityCreationException("Unable to create entity of type ["+getJavaClass()+"]: " + e.getMessage(),e);
        } catch (IllegalAccessException e) {
            throw new EntityCreationException("Unable to create entity of type ["+getJavaClass()+"]: " + e.getMessage(),e);
        }
    }

    public String getName() {
        return javaClass.getName();
    }

    public PersistentProperty getIdentity() {
        return this.identity;
    }

    public Class getJavaClass() {
        return this.javaClass;
    }

    public boolean isInstance(Object obj) {
        return getJavaClass().isInstance(obj);
    }

    public List<PersistentProperty> getPersistentProperties() {
        return persistentProperties;
    }

    public List<Association> getAssociations() {
        return associations;
    }

    public PersistentProperty getPropertyByName(String name) {
        return propertiesByName.get(name);
    }

    @Override
    public int hashCode() {
        return javaClass.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PersistentEntity)) return false;
        if(this == o) return true;

        PersistentEntity other = (PersistentEntity) o;
        return javaClass.equals(other.getJavaClass());
    }

    @Override
    public String toString() {
        return javaClass.getName();
    }
}
