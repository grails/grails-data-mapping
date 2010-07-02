package org.springframework.datastore.groovy

import org.junit.Test
import org.springframework.datastore.cassandra.CassandraDatastore
import org.junit.BeforeClass
import org.apache.cassandra.service.EmbeddedCassandraService
import org.apache.cassandra.contrib.utils.service.CassandraServiceDataCleaner
import org.springframework.core.io.ClassPathResource

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class SimplePersistenceMethodsTests  {

  protected static EmbeddedCassandraService cassandra;

   @BeforeClass
   public static void setupCassandra() {
    // Tell cassandra where the configuration files are.
         // Use the test configuration file.
         try {

           System.setProperty("storage-config", new ClassPathResource("cassandra-conf").file.absolutePath)
           CassandraServiceDataCleaner cleaner = new CassandraServiceDataCleaner()
           cleaner.prepare()
           cassandra = new EmbeddedCassandraService()
           cassandra.init()
           Thread t = new Thread(cassandra)
           t.setDaemon(true)
           t.start()
         } catch (Throwable e) {
             e.printStackTrace()
             println("Failed to setup Cassandra ${e.message}")
         };


   }

  @Test
  void testBasicCRUD() {
    def ds = new CassandraDatastore()
    ds.mappingContext.addPersistentEntity(TestEntity)

    def mop = new RuntimeCapabilities(ds)
    mop.enhance()
    def conn = ds.connect(null)

    try {
      def test = new TestEntity(name:"bob", age:45)
      test.save()
      assert test.id != null
      def test2 = TestEntity.get(test.id)
      assert test2 != null
      assert test.id == test2.id
      assert "bob" == test.name
      assert 45 == test.age
      test.delete()
    } finally {
      conn.disconnect()
    }

  }

}
class TestEntity {
  UUID id
  String name
  int age
}
