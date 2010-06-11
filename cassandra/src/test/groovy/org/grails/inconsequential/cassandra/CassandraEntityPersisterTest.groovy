package org.grails.inconsequential.cassandra

import org.junit.Test
import static org.junit.Assert.*
import org.grails.inconsequential.core.ObjectDatastoreConnection
import org.grails.inconsequential.cassandra.uuid.UUIDUtil;
/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CassandraEntityPersisterTest extends AbstractCassandraTest {

  @Test
  void testReadWrite() {
     def ds = new CassandraDatastore()
     ds.mappingContext.addPersistentEntity(TestEntity)
     ObjectDatastoreConnection conn = ds.connect(null)

     def t = conn.retrieve(TestEntity, new CassandraKey(UUIDUtil.getTimeUUID()))

     assert t == null
  }
}
class TestEntity {
  UUID id
  String name
}
