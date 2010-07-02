package org.springframework.datastore.cassandra


import org.junit.Test
import org.springframework.datastore.cassandra.uuid.UUIDUtil
import org.springframework.datastore.core.Session

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CassandraEntityPersisterTest extends AbstractCassandraTest {

  @Test
  void testReadWrite() {
     def ds = new CassandraDatastore()
     ds.mappingContext.addPersistentEntity(TestEntity)
     Session conn = ds.connect(null)

     def t = conn.retrieve(TestEntity, UUIDUtil.getTimeUUID())

     assert t == null

     t = new TestEntity(name:"Bob", age:45)

     conn.persist(t)

     assert t.id != null

     t = conn.retrieve(TestEntity, t.id)

     assert t != null
     assert "Bob" == t.name
     assert 45 == t.age
     assert t.id != null


     t.age = 55
     conn.persist(t)

     t = conn.retrieve(TestEntity, t.id)

     assert 55 == t.age

  }

}
class TestEntity {
  UUID id
  String name
  int age
}
