package org.springframework.datastore.jcr

import org.junit.Test
import org.junit.Ignore

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrEntityPersisterTests extends AbstractJcrTest {

  @Ignore
  void testConnection(){
    //Session conn = ds.connect(connectionDetails)
    assert null != conn;
    assert true == conn.isConnected();

    def session = conn.getNativeInterface();
    assert null != session;

    //conn.disconnect(); //disconnect method still doesn't work
    //assert false == conn.isConnected();
  }

  @Test
  void testPersist(){
    ds.mappingContext.addPersistentEntity(TestEntity); //Why it needed?
    def t = new TestEntity(title:"foo",body:"bar");
    conn.persist(t);
    assert null != t.id;
    println t.id;
   
  }


}

class TestEntity{
   String version
   String path
   //String UUID  should be in id - transform needed

   UUID id
   String title
   String body
}
