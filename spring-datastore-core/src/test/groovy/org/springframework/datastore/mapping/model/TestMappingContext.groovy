package org.springframework.datastore.mapping.model

import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TestMappingContext extends AbstractMappingContext {
  MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(new TestMappedPropertyFactory())
  MappingFactory mappingFactory = new TestMappedPropertyFactory()

  protected PersistentEntity createPersistentEntity(Class javaClass) {
    return new TestPersistentEntity(javaClass, this)
  }

}
