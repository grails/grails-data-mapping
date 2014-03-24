package org.grails.datastore.mapping.cassandra

import org.junit.BeforeClass
import org.springframework.core.io.ClassPathResource

/**
 * Test harness for Cassandra tests
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class AbstractCassandraTest {

//    protected static EmbeddedCassandraService cassandra

    @BeforeClass
    static void setupCassandra() {
        // Tell cassandra where the configuration files are.
        // Use the test configuration file.
        try {
//            System.setProperty("storage-config", new ClassPathResource("cassandra-conf").file.absolutePath)
//            new CassandraServiceDataCleaner().prepare()
//            cassandra = new EmbeddedCassandraService()
//            cassandra.init()
//            Thread t = new Thread(cassandra)
//            t.setDaemon(true)
//            t.start()
        }
        catch (Throwable e) {
            e.printStackTrace()
            println("Failed to setup Cassandra ${e.message}")
        }
    }
}
