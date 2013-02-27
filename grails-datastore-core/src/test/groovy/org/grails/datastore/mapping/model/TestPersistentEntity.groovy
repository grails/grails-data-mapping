package org.grails.datastore.mapping.model

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@SuppressWarnings("unchecked")
class TestPersistentEntity extends AbstractPersistentEntity {
    private TestClassMapping classMapping

    public TestPersistentEntity(Class type, MappingContext ctx) {
        super(type, ctx);
        classMapping = new TestClassMapping(this, ctx);
    }

    @Override
    public ClassMapping getMapping() {
        return classMapping;
    }

    public class TestClassMapping extends AbstractClassMapping<String> {
        private String mappedForm;

        public TestClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
            mappedForm = context.mappingFactory.createMappedForm(TestPersistentEntity.this);
         }

        @Override
        public String getMappedForm() {
            return mappedForm;
        }
    }
}
