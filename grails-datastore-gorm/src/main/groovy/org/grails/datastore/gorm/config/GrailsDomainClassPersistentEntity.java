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

package org.grails.datastore.gorm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.PropertyMapping;
import org.springframework.datastore.mapping.model.lifecycle.Initializable;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.model.types.Embedded;
import org.springframework.datastore.mapping.model.types.ManyToMany;
import org.springframework.datastore.mapping.model.types.ManyToOne;
import org.springframework.datastore.mapping.model.types.OneToMany;
import org.springframework.datastore.mapping.model.types.OneToOne;

/**
 * Bridges the {@link GrailsDomainClass} interface into the {@link PersistentEntity} interface
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class GrailsDomainClassPersistentEntity implements PersistentEntity, Initializable {

	private GrailsDomainClass domainClass;
	private GrailsDomainClassMappingContext mappingContext;
	private GrailsDomainClassPersistentProperty identifier;
	private Map<String, PersistentProperty> propertiesByName = new HashMap<String, PersistentProperty>();
	private List<PersistentProperty> properties = new ArrayList<PersistentProperty>();
	private List<Association> associations = new ArrayList<Association>();
	
	public GrailsDomainClassPersistentEntity(GrailsDomainClass domainClass,
			GrailsDomainClassMappingContext mappingContext) {
		super();
		this.domainClass = domainClass;
		this.mappingContext = mappingContext;
	}

	/**
	 * @return The wrapped GrailsDomainClass instance
	 */
	public GrailsDomainClass getDomainClass() {
		return domainClass;
	}

	@Override
	public void initialize() {
		final GrailsDomainClassProperty identifier = domainClass.getIdentifier();
		this.identifier = new GrailsDomainClassPersistentProperty(this, identifier);
		
		mappingContext.addEntityValidator(this, domainClass.getValidator());
		
		final GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties();
		for (GrailsDomainClassProperty grailsDomainClassProperty : persistentProperties) {
			PersistentProperty persistentProperty;
			if(grailsDomainClassProperty.isAssociation()) {
				if(grailsDomainClassProperty.isEmbedded()) {
					persistentProperty = createEmbedded(mappingContext,grailsDomainClassProperty);
				}
				else if(grailsDomainClassProperty.isOneToMany()) {
					persistentProperty = createOneToMany(mappingContext, grailsDomainClassProperty);
				}
				else if(grailsDomainClassProperty.isHasOne()) {
					persistentProperty = createOneToOne(mappingContext, grailsDomainClassProperty);
				}
				else if(grailsDomainClassProperty.isOneToOne()) {
					persistentProperty = createOneToOne(mappingContext, grailsDomainClassProperty);
				}
				else if(grailsDomainClassProperty.isManyToOne()) {
					persistentProperty = createManyToOne(mappingContext, grailsDomainClassProperty);
				}
				else if(grailsDomainClassProperty.isManyToMany()) {
					persistentProperty = createManyToMany(mappingContext, grailsDomainClassProperty);
				}
				else {
					persistentProperty = new GrailsDomainClassPersistentProperty(this, grailsDomainClassProperty);
				}
				associations.add((Association) persistentProperty);
			}
			else {
				persistentProperty = new GrailsDomainClassPersistentProperty(this, grailsDomainClassProperty);
			}
			propertiesByName.put(grailsDomainClassProperty.getName(), persistentProperty);
			properties.add(persistentProperty);
		}
	}

	@Override
	public String getName() {
		return domainClass.getFullName();
	}

	@Override
	public PersistentProperty getIdentity() {
		return identifier;
	}

	@Override
	public List<PersistentProperty> getPersistentProperties() {
		return properties;
	}

	@Override
	public List<Association> getAssociations() {
		return associations;
	}

	@Override
	public PersistentProperty getPropertyByName(String name) {
		return propertiesByName.get(name);
	}

	@Override
	public Class getJavaClass() {
		return domainClass.getClazz();
	}

	@Override
	public boolean isInstance(Object obj) {
		return domainClass.getClazz().isInstance(obj);
	}

	@Override
	public ClassMapping getMapping() {
		return null;
	}

	@Override
	public Object newInstance() {
		return domainClass.newInstance();
	}

	@Override
	public List<String> getPersistentPropertyNames() {
		return new ArrayList<String>( propertiesByName.keySet() );
	}

	@Override
	public String getDecapitalizedName() {
		return domainClass.getLogicalPropertyName();
	}

	@Override
	public boolean isOwningEntity(PersistentEntity owner) {
		return domainClass.isOwningClass(owner.getJavaClass());
	}

	@Override
	public PersistentEntity getParentEntity() {
		if(!isRoot()) {
			return getMappingContext()
				.getPersistentEntity(
							getJavaClass()
							.getSuperclass()
							.getName());
		}
		return null;
	}

	@Override
	public PersistentEntity getRootEntity() {
		if(isRoot()) return this;
		else {
			PersistentEntity parent = getParentEntity();
			while(!parent.isRoot()) {
				parent = parent.getParentEntity();
			}
			return parent;
		}
	}

	@Override
	public boolean isRoot() {
		return domainClass.isRoot();
	}

	@Override
	public String getDiscriminator() {
		return getName();
	}

	@Override
	public MappingContext getMappingContext() {
		return mappingContext;
	}

	@Override
	public boolean hasProperty(String name, Class type) {
		return domainClass.hasProperty(name);
	}

	@Override
	public boolean isIdentityName(String propertyName) {
		return domainClass.getIdentifier().getName().equals(propertyName);
	}

	


	private PersistentProperty createManyToOne(
			GrailsDomainClassMappingContext ctx,
			GrailsDomainClassProperty grailsDomainClassProperty) {
		final ManyToOne oneToOne = new ManyToOne(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
			@Override
			public PropertyMapping getMapping() {
				return null;
			}
			
		};
		configureAssociation(grailsDomainClassProperty, oneToOne);
		return oneToOne;
	}	
	

	private PersistentProperty createManyToMany(
			GrailsDomainClassMappingContext ctx,
			GrailsDomainClassProperty grailsDomainClassProperty) {
		final ManyToMany oneToOne = new ManyToMany(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
			@Override
			public PropertyMapping getMapping() {
				return null;
			}
			
		};
		configureAssociation(grailsDomainClassProperty, oneToOne);
		return oneToOne;
	}		
	
	private PersistentProperty createOneToOne(
			GrailsDomainClassMappingContext ctx,
			GrailsDomainClassProperty grailsDomainClassProperty) {
		final OneToOne oneToOne = new OneToOne(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
			@Override
			public PropertyMapping getMapping() {
				return null;
			}
			
		};
		configureAssociation(grailsDomainClassProperty, oneToOne);
		return oneToOne;
	}

	private OneToMany createOneToMany(GrailsDomainClassMappingContext mappingContext,
			GrailsDomainClassProperty grailsDomainClassProperty) {
		final OneToMany oneToMany = new OneToMany(this, mappingContext, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {

			@Override
			public PropertyMapping getMapping() {
				return null;
			}
		};
		configureAssociation(grailsDomainClassProperty, oneToMany);
		
		return oneToMany;
	}

	private void configureAssociation(
			GrailsDomainClassProperty grailsDomainClassProperty,
			final Association association) {
		association.setAssociatedEntity(getMappingContext().addPersistentEntity(grailsDomainClassProperty.getReferencedPropertyType()));
		association.setOwningSide(grailsDomainClassProperty.isOwningSide());
		association.setReferencedPropertyName(grailsDomainClassProperty.getReferencedPropertyName());
	}

	private PersistentProperty createEmbedded(
			GrailsDomainClassMappingContext mappingContext,
			GrailsDomainClassProperty grailsDomainClassProperty) {
		Embedded persistentProperty = new Embedded(this, mappingContext, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getClass()) {
			@Override
			public PropertyMapping getMapping() {
				return null;
			}
			
		};
		persistentProperty.setOwningSide(grailsDomainClassProperty.isOwningSide());
		persistentProperty.setReferencedPropertyName(grailsDomainClassProperty.getReferencedPropertyName());
		
		return persistentProperty;
	}
	
}
