package org.springframework.datastore.mapping.jcr

import org.junit.Test
import grails.persistence.Entity

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class OneToOneAssociationTests extends AbstractJcrTest {

  @Test
  void testPersistOneToOneAssociation() {
    ds.mappingContext.addPersistentEntity(Blog)

    def b = new Blog(author: "Greame Rocher")
    b.post = new Post(title: "foo", text: "bar")

    conn.persist(b)

    b = conn.retrieve(Blog, b.id)

    assert b != null
    assert "Greame Rocher" == b.author
    assert b.post != null
    assert "foo" == b.post.title
    assert "bar" == b.post.text

    def id = b.id;

    conn.delete(b)
    conn.flush()

    b = conn.retrieve(Blog, id)

    assert null == b

   }
}

@Entity
class Blog {
  String id
  String author
  Post post
}
@Entity
class Post {
  String id
  String title
  String text
}


