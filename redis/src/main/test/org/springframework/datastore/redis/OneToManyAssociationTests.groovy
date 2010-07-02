package org.springframework.datastore.redis

import grails.persistence.Entity
import org.springframework.datastore.core.ObjectDatastoreConnection
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class OneToManyAssociationTests {

  @Test
  void testOneToManyAssociation() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Author)
    ObjectDatastoreConnection conn = ds.connect(null)

    def a = new Author(name:"Stephen King")
    a.books = [ new Book(title:"The Stand"), new Book(title:"It")] as Set

    conn.persist(a)

    a = conn.retrieve(Author, new RedisKey(a.id))

    assert a != null
    assert "Stephen King" == a.name
    assert a.books != null
    assert 2 == a.books.size()
    
    def b1 = a.books.find { it.title == 'The Stand'}
    assert b1 != null
    assert b1.id != null
    assert "The Stand" == b1.title
    
  }
}

@Entity
class Author {
  Long id
  String name
  Set books
  static hasMany = [books:Book]
}
@Entity
class Book {
  Long id
  String title
}
