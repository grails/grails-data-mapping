package org.grails.datastore.mapping.keyvalue.mapping

import org.grails.datastore.mapping.keyvalue.mapping.config.Family
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity
import org.junit.Before
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class KeyValueMappingFactoryTests {
    def context

    @Before
    void setUp() {
        context = new KeyValueMappingContext("myspace")
        context.addPersistentEntity(TestEntity)
        context.addPersistentEntity(AbstractTestEntity)
    }

    @Test
    void testCreateMappedForm() {
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

    @Test
    void testParentEntity() {
        KeyValuePersistentEntity entity = context.getPersistentEntity(TestEntity.name)
        assert entity != null
        assert !entity.root
        assert entity.parentEntity != null
        assert entity.rootEntity != null
    }

    abstract class AbstractTestEntity {
        Long id
    }

    class TestEntity extends AbstractTestEntity {
        Long version
    }
}
