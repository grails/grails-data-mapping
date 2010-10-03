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

  @Ignore
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

  /*  t = conn.retrieve(TestEntity, t.id)

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
    assert t == null*/
  }

  @Test
  void deleteInstances() {
    def session = conn.getNativeInterface();
    if (session.itemExists("/TestEntity")) {
        session.getRootNode().getNode("TestEntity").getNodes().each {
        it.remove()
      }
      session.save()
    }
    assert false == session.itemExists("/TestEntity");

  }
}



class TestEntity {
  //using id field as based APIs required,
  //the JCR generated UUID will be assigned to the id property.
  String id
  String version

  String path
  String title
  String body
}
