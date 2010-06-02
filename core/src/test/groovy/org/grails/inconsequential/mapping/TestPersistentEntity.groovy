package org.grails.inconsequential.mapping

import org.grails.inconsequential.mapping.types.Identity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TestPersistentEntity extends AbstractPersistentEntity{

  TestPersistentEntity(Class javaClass, MappingContext context) {
    super(javaClass, context);    
  }


}
