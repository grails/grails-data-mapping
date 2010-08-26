package org.springframework.datastore.mapping

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TestPersistentEntity extends AbstractPersistentEntity{

  TestPersistentEntity(Class javaClass, MappingContext context) {
    super(javaClass, context);    
  }


}
