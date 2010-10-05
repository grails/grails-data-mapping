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

   b = conn.retrieve(Post, b.id)

    assert b != null
    assert "Greame Rocher" == b.author
    assert b.post != null
    assert "foo" == b.post.title
    assert "bar" == b.post.text
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


