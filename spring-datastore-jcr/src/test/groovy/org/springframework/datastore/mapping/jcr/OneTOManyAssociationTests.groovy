package org.springframework.datastore.mapping.jcr

import grails.persistence.Entity
import org.junit.Test
import javax.jcr.Session

/**
 * @author Erawat Chamanont
 * @since 1.1
 */
class OneToManyAssociationTests extends AbstractJcrTest {

  @Test
  void testOneToManyAssociation() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Scott Davis")
    a.books = [new Book(title: "Groovy Recipes"), new Book(title: "JBoss at Work")] as Set
    conn.persist(a)
    conn.flush()

    a = conn.retrieve(Author, a.id)

    assert null != a.id
    assert "Scott Davis" == a.name
    assert null != a.books
    assert 2 == a.books.size()

    def b1 = a.books.find { it.title == 'Groovy Recipes'}
    assert b1 != null
    assert b1.id != null
    assert "Groovy Recipes" == b1.title

    def b2 = a.books.find { it.title == 'JBoss at Work'}
    assert null != b2.id
    assert "JBoss at Work" == b2.title

    Session session = conn.getNativeInterface()
    if (session.itemExists("/Author")) {
      print 'true';
      session.getRootNode().getNode("Author").getNodes().each {
        it.remove()
      }
      session.save()
    }


  }

}

@Entity
class Author {
  String id
  String name
  Set books
  static hasMany = [books: Book]
}
@Entity
class Book {
  String id
  String title
}

