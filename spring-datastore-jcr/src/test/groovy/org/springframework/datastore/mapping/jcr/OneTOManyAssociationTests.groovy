package org.springframework.datastore.mapping.jcr

import grails.persistence.Entity
import org.junit.Test

/**
 * @author Erawat Chamanont
 * @since 1.1
 */
class OneToManyAssociationTests extends AbstractJcrTest {
  @Test
  void testOneToManyAssociation() {

  }

}

@Entity
class Author {
  String id
  String name
  Set books
  static hasMany = [books:Book]
}
@Entity
class Book {
  String id
  String title
}

