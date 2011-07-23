package org.grails.datastore.mapping.model

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@SuppressWarnings("unchecked")
class TestPersistentEntity extends AbstractPersistentEntity {
    TestPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context)
    }
}
