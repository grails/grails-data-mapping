package org.grails.inconsequential.redis

import org.grails.inconsequential.core.ObjectDatastoreConnection
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class RedisEntityPesisterTests {

  @Test
  void testPersistObject() {
    RedisDatastore ds = new RedisDatastore()

    ObjectDatastoreConnection conn = ds.connect(null)

    try {
      conn.clear()
      ds.getMappingContext().addPersistentEntity(TestEntity)

//      assert conn.retrieve(TestEntity, new RedisKey(1)) == null
      
      TestEntity t = new TestEntity()
      t.name = "bob"
      conn.persist(t)

      assert t.id != null

      def key = new RedisKey(t.id)
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
}
class TestEntity {
  Long id
  String name
}
