package org.springframework.datastore.redis

import org.junit.Test
import org.springframework.datastore.core.Session

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class RedisEntityPesisterTests {

  @Test
  void testPersistObject() {
    RedisDatastore ds = new RedisDatastore()

    Session conn = ds.connect(null)

    try {
      conn.clear()
      ds.getMappingContext().addPersistentEntity(TestEntity)

//      assert conn.retrieve(TestEntity, new RedisKey(1)) == null
      
      TestEntity t = new TestEntity()
      t.name = "bob"
      conn.persist(t)

      assert t.id != null

      def key = t.id
      t = conn.retrieve(TestEntity, key)

      assert t != null
      assert "bob"  == t.name

      conn.delete(t)

      t = conn.retrieve(TestEntity, key)

      assert t == null

    }
    finally {
      conn.disconnect()
    }
  }


  @Test
  void testTransactions() {

    // doesn't work right now
    def ds = new RedisDatastore()
    ds.getMappingContext().addPersistentEntity(TestEntity)

    Session conn = ds.connect(null)
    conn.clear()

    assert 0 == conn.size()

    def t = conn.beginTransaction()
    TestEntity te = new TestEntity()
    te.name = "bob"
    conn.persist(te)
    TestEntity te2 = new TestEntity()
    te2.name = "frank"
    conn.persist(te2)
    t.commit()

    assert 2 == conn.size()

    t = conn.beginTransaction()
    TestEntity te3 = new TestEntity()
    te3.name = "joe"
    conn.persist(te3)
    TestEntity te4 = new TestEntity()
    te4.name = "jack"
    conn.persist(te4)

    t.rollback()

    assert 2 == conn.size()

  }
}
class TestEntity {
  Long id
  String name
}
