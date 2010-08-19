package org.springframework.datastore.jcr

import org.junit.Test
import org.junit.Ignore
import javax.jcr.Value
import javax.jcr.PropertyIterator
import javax.jcr.Property

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

    conn.disconnect(); //doesn't work, the session is still alive.
    assert false == conn.isConnected();
  }

  @Test
  void testPersist() {
    ds.mappingContext.addPersistentEntity(TestEntity);
    def t = new TestEntity(title: "foo", body: "bar");
    conn.persist(t);
    assert null != t.id;
    println t.id;
  } 

}

class TestEntity {
  String id

  String title
  String body
}
