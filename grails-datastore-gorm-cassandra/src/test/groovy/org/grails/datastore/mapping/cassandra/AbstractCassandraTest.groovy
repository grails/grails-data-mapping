package org.grails.datastore.mapping.cassandra

import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.junit.BeforeClass
import org.springframework.context.support.GenericApplicationContext

/**
 * Test harness for Cassandra tests
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractCassandraTest {

	//protected static EmbeddedCassandraService cassandra
	protected static CassandraDatastore datastore
	protected static CassandraSession  session
	protected static keyspace = "unittest"
	@BeforeClass
	static void setupCassandra() {
		// Tell cassandra where the configuration files are.
		// Use the test configuration file.

		def ctx = new GenericApplicationContext()
		ctx.refresh()

		ConfigObject config = new ConfigObject()
		datastore = new CassandraDatastore(new CassandraMappingContext(keyspace), config, ctx)
		datastore.afterPropertiesSet()
		session = datastore.connect()

		def nativeSession = session.getNativeInterface()

		try {
			nativeSession.execute("""
                CREATE KEYSPACE ${keyspace} WITH replication = {
                    'class': 'SimpleStrategy',
                    'replication_factor': '1'
                };
                """)
		} catch (AlreadyExistsException)
		{
			
		}

	}
}
