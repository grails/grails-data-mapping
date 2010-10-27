package org.springframework.datastore.mapping.jcr

import org.junit.Test
import javax.jcr.Session

/**
 * Tests for locking
 * @author Erawat Chamanont
 * @since 1.0
 */
class LockTests extends AbstractJcrTest{

  @Test
  void testLock() {
    ds.mappingContext.addPersistentEntity(TestEntity)
    def t = new TestEntity(title: "foo", body: "bar")
    conn.persist(t)
    conn.flush()

    assert null != t.id;

    Session session = conn.getNativeInterface();
    def node = session.getNodeByUUID(t.id)

    conn.lock(t);

    assert true  == node.isLocked()

    conn.unlock(t);

    assert false == node.isLocked()

    
  }
}


