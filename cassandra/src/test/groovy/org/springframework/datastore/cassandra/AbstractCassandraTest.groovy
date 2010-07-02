package org.springframework.datastore.cassandra

import org.junit.BeforeClass

import org.apache.cassandra.service.EmbeddedCassandraService
import org.apache.cassandra.contrib.utils.service.CassandraServiceDataCleaner
import org.apache.cassandra.thrift.Cassandra
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.TSocket
import org.apache.thrift.transport.TTransport
import org.apache.thrift.transport.TTransportException

import org.springframework.core.io.ClassPathResource;

/**
 * Test harness for Cassandra tests
 *
 * @author Graeme Rocher
 * @since 1.0
 */

class AbstractCassandraTest {

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

   /**
     * Gets a connection to the localhost client
     *
     * @return
     * @throws TTransportException
     */
    protected Cassandra.Client getClient() throws TTransportException {
      
        TTransport tr = new TSocket("localhost", 9170);
        TProtocol proto = new TBinaryProtocol(tr);
        Cassandra.Client client = new Cassandra.Client(proto);
        tr.open();
        return client;
    }
}
