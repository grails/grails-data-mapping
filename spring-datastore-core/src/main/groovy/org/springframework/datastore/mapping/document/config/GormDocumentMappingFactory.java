package org.springframework.datastore.mapping.document.config;

import org.springframework.datastore.mapping.config.AbstractGormMappingFactory;

public class GormDocumentMappingFactory extends AbstractGormMappingFactory<Collection, Attribute> {

    @Override
    protected Class<Attribute> getPropertyMappedFormType() {
        return Attribute.class;
    }

    @Override
    protected Class<Collection> getEntityMappedFormType() {
        return Collection.class;
    }
}
