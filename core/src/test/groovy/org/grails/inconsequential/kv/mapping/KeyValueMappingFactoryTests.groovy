package org.grails.inconsequential.kv.mapping

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class KeyValueMappingFactoryTests {

  @Test
  void testCreateMappedForm() {
    def context = new KeyValueMappingContext("myspace")

    context.addPersistentEntity(TestEntity)
    KeyValuePersistentEntity entity = context.getPersistentEntity(TestEntity.name)

    assert entity != null

    Family entityMapping = entity.mapping.mappedForm
    assert "myspace" == entityMapping.keyspace
    assert TestEntity.name == entityMapping.family
    assert "id" == entity.mapping.identifier.identifierName[0]

    KeyValue kv = entity.identity.mapping.mappedForm
    assert kv != null
    assert kv.key == 'id'
  }

  class TestEntity {
    Long id
    Long version
  }
}
