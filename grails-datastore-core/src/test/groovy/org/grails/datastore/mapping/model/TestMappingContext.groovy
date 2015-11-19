package org.grails.datastore.mapping.model

import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TestMappingContext extends AbstractMappingContext {
    MappingFactory mappingFactory = new TestMappedPropertyFactory()
    MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)

    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new TestPersistentEntity(javaClass, this)
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass, boolean external) {
        return new TestPersistentEntity(javaClass, this)
    }
}
