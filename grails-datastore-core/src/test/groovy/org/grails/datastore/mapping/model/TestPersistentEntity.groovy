package org.grails.datastore.mapping.model

import org.grails.datastore.mapping.config.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@SuppressWarnings("unchecked")
class TestPersistentEntity extends AbstractPersistentEntity {
    private TestClassMapping classMapping

    TestPersistentEntity(Class type, MappingContext ctx) {
        super(type, ctx)
        classMapping = new TestClassMapping(this, ctx)
    }

    @Override
    ClassMapping getMapping() { classMapping }

    public class TestClassMapping extends AbstractClassMapping<Entity> {
        private Entity mappedForm;

        TestClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context)
            mappedForm = context.mappingFactory.createMappedForm(TestPersistentEntity.this)
         }

        @Override
        public Entity getMappedForm() {
            return mappedForm;
        }
    }
}
