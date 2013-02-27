package org.grails.datastore.mapping.model

import org.grails.datastore.mapping.config.AbstractGormMappingFactory
import org.grails.datastore.mapping.config.Property

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TestMappedPropertyFactory extends AbstractGormMappingFactory {

    @Override
    protected Class getPropertyMappedFormType() {
        Property
    }

    @Override
    protected Class getEntityMappedFormType() {
        String
    }
}
