package org.springframework.datastore.mapping.document.config;

import org.springframework.datastore.mapping.config.AbstractGormMappingFactory;
import org.springframework.datastore.mapping.config.Property;

public class GormDocumentMappingFactory extends AbstractGormMappingFactory<Collection, Property> {

	@Override
	protected Class<Property> getPropertyMappedFormType() {
		return Property.class;
	}

	@Override
	protected Class<Collection> getEntityMappedFormType() {
		return Collection.class;
	}

}
