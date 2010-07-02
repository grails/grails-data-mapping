package org.springframework.datastore.mapping

import org.springframework.datastore.mapping.syntax.GormMappingSyntaxStrategy

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TestMappingContext extends AbstractMappingContext {
  MappingSyntaxStrategy mappingSyntaxStrategy = new GormMappingSyntaxStrategy(new TestMappedPropertyFactory())
  MappingFactory mappingFactory = new TestMappedPropertyFactory()

  protected PersistentEntity createPersistentEntity(Class javaClass) {
    return new TestPersistentEntity(javaClass, this)
  }

}
