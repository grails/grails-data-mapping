package org.grails.datastore.mapping.keyvalue.mapping

import org.grails.datastore.mapping.keyvalue.mapping.config.Family
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class KeyValueMappingFactoryTests {
    def context

    @BeforeEach
    void setUp() {
        context = new KeyValueMappingContext("myspace")
        context.addPersistentEntity(TestEntity)
        context.addPersistentEntity(AbstractTestEntity)
        context.addPersistentEntity(FormulaTestEntity)
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
    void testEntityWithFormula() {
        KeyValuePersistentEntity entity = context.getPersistentEntity(FormulaTestEntity.name)
        assert entity != null

        KeyValue kv = entity.identity.mapping.mappedForm as KeyValue
        assert kv != null
        assert kv.key == 'id'

        PersistentProperty prop = entity.getPropertyByName('nonFormulaProperty')
        assert !prop.mapping.mappedForm.derived
        assert !prop.mapping.mappedForm.nullable

        // Formula properties should be flagged as derived
        prop = entity.getPropertyByName('formulaProperty')
        assert prop.mapping.mappedForm.derived
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

    class FormulaTestEntity extends AbstractTestEntity {
        String nonFormulaProperty
        String formulaProperty

        static mapping = {
            formulaProperty(formula: 'foo(bar)')
        }
    }
}
