package org.grails.inconsequential.mapping

import org.junit.Test
import org.grails.inconsequential.mapping.syntax.GormMappingSyntaxStrategy
import javax.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class GormMappingSyntaxTests {

  @Test
  void testIsEntity() {

    def strategy = new GormMappingSyntaxStrategy(new TestMappedPropertyFactory())

    assert strategy.isPersistentEntity(TestEntity)
    assert strategy.isPersistentEntity(JavaEntity)
    assert !strategy.isPersistentEntity(GormMappingSyntaxTests)
  }

  @Test
  void testGetIdentity() {
    def context = new TestMappingContext()
    context.addPersistentEntity(TestEntity)
    def strategy = context.mappingSyntaxStrategy
    def id = strategy.getIdentity(TestEntity, context)

    assert id != null

    assert id.type == Long
    assert id.name == 'id'
  }

  @Entity
  class JavaEntity {

  }
  class TestEntity {
    Long id
    Long version
  }

}
