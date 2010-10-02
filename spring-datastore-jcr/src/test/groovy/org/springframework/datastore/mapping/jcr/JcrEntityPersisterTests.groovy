package org.springframework.datastore.mapping.jcr

import org.junit.Test
import org.junit.Ignore
import javax.jcr.Value
import javax.jcr.PropertyIterator
import javax.jcr.Property
import javax.jcr.Session

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrEntityPersisterTests extends AbstractJcrTest {

  //@Ignore
  @Test
  void testConnection() {
    assert null != conn;
    assert true == conn.isConnected();

    def session = conn.getNativeInterface();
    assert null != session;

  }

  @Test
  void testPersist() {
    ds.mappingContext.addPersistentEntity(TestEntity);
    def t = new TestEntity(title: "foo", body: "bar");
    conn.persist(t);
    assert null != t.id;

    t = conn.retrieve(TestEntity, t.id)

    assert t != null
    assert "foo" == t.title
    assert "bar" == t.body
    assert null != t.id

    t.title = 'blog'
    conn.persist(t)
    t = conn.retrieve(TestEntity, t.id)
    assert 'blog' == t.title
    assert 'bar' == t.body

    def id = t.id

    conn.delete(t)
    t = conn.retrieve(TestEntity, id)
    assert t == null
  }

  @Test(dependsOnMethods = ["testPersist"])
  void deleteInstances() {
    def session = conn.getNativeInterface();
    if (session.itemExists("/TestEntity")) {
        session.getRootNode().getNode("TestEntity").getNodes().each {
        it.remove()
      }
      session.save()
    }

  }
}



class TestEntity {
  static mapWith = 'jcr'
  static namespace = 'blog'

  Long id
  String version

  String path
  String UUID
  String title
  String body
}
