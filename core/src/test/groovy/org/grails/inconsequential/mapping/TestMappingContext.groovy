package org.grails.inconsequential.mapping

import org.grails.inconsequential.mapping.syntax.GormMappingSyntaxStrategy

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
