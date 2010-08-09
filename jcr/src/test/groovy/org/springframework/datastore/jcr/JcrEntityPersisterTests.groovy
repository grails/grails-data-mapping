package org.springframework.datastore.jcr

import org.junit.Test
import org.springframework.datastore.core.Session

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrEntityPersisterTests extends AbstractJcrTest {

  @Test
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

  }
}

class TestEntity{
   UUID id
   String name
   int age
}
