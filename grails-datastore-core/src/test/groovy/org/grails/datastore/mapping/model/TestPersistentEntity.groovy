package org.grails.datastore.mapping.model

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

    class TestClassMapping extends AbstractClassMapping<String> {
        private String mappedForm

        TestClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context)
            mappedForm = context.mappingFactory.createMappedForm(TestPersistentEntity.this)
         }

        @Override
        String getMappedForm() { mappedForm }
    }
}
