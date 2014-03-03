package org.grails.datastore.gorm

import com.datastax.driver.core.Cluster
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext

import java.nio.ByteBuffer
import org.grails.datastore.gorm.cassandra.CassandraGormEnhancer
import org.grails.datastore.gorm.cassandra.CassandraMethodsConfigurer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datstore.gorm.cassandra.CassandraMethodsConfigurer
import org.springframework.context.support.GenericApplicationContext

import java.lang.reflect.Modifier

class Setup {

	static destroy() {
		// noop
	}

	static Session setup(List<Class> classes) {
		def ctx = new GenericApplicationContext()
		ctx.refresh()

		def conf = new ConfigObject()
		conf.setProperty('contactPoints',['jeff-cassandra.dev.wh.reachlocal.com'])
		def ds = new CassandraDatastore(new KeyValueMappingContext(CassandraDatastore.DEFAULT_KEYSPACE), ctx, conf)
//		ds.applicationContext = ctx


		for (cls in classes) {
			ds.mappingContext.addPersistentEntity(cls)
			createOrCleanTable(cls, ds)
		}

		def txMgr = new DatastoreTransactionManager(datastore: ds)
		CassandraMethodsConfigurer methodsConfigurer = new CassandraMethodsConfigurer(ds, txMgr)
		methodsConfigurer.configure()


		def enhancer = new CassandraGormEnhancer(ds)

		ds.mappingContext.addMappingContextListener({ e ->
			enhancer.enhance e
			println "enhance " + e
		} as MappingContext.Listener)

		ds.applicationContext.addApplicationListener new DomainEventListener(ds)
		ds.applicationContext.addApplicationListener new AutoTimestampEventListener(ds)

		return ds.connect()
	}

	private static void createOrCleanTable(Class clazz, CassandraDatastore ds) {
		println "Configuring $clazz"

		//TODO deal with keyspace creation?

		def tableName = clazz.getSimpleName()

		//TODO check if table is there if it is clear if not create

		com.datastax.driver.core.Session session = ds.connect().getNativeInterface()

		String dropTable = "DROP TABLE IF EXISTS ${ds.DEFAULT_KEYSPACE}.${tableName};"


		String createTable = "CREATE TABLE IF NOT EXISTS ${ds.DEFAULT_KEYSPACE}.${tableName} ("

		boolean hadId = false
		boolean first = true
		for (def field in clazz.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && field.name != "metaClass") {
				//				println ">" + field.name + ": " + field.type
				String toAdd = ""
				if (field.name == 'id') {
					toAdd = "id uuid PRIMARY KEY"
					hadId = true
				} else {
					def type = getCassandraType(field.type)
					toAdd = " \"${field.name.toLowerCase()}\" $type"
				}

				if (first) {
					first = false
				} else {
					toAdd = "," + toAdd
				}

				createTable += toAdd
			}
		}

		if (!hadId) {

			if (!first) {
				createTable += ","
			}
			createTable += "id uuid PRIMARY KEY"
		}
		createTable += ");"

		println dropTable
		println createTable
		session.execute(dropTable)
		session.execute(createTable);

	}

	private static String getCassandraType(Class c) {

		String cassandraType = "blob"

		if (c.is(String.class)) {
			cassandraType = "text"
		} else if (c.equals(long.class) || c.equals(Long)) {
			cassandraType = "bigint"
		} else if (c.equals(ByteBuffer.class)) {
			cassandraType = "blob"
		} else if (c.equals(boolean.class)) {
			cassandraType = "boolean"
		} else if (c.equals(BigDecimal.class)) {
			cassandraType = "decimal"
		} else if (c.equals(double.class)) {
			cassandraType = "double"
		} else if (c.equals(float.class)) {
			cassandraType = "float"
		} else if (c.equals(int.class) || c.equals(Integer)) {
			cassandraType = "int"
		} else if (c.equals(List.class)) {
			cassandraType = "list<text>"
		} else if (c.equals(Map.class)) {
			cassandraType = "map<text,text>"
		} else if (c.equals(Set.class)) {
			//TODO reflection to find generic type? Should sets vs hasmany be handled a certian way
			cassandraType = "set<text>"
		} else if (c.equals(String.class)) {
			cassandraType = "text"
		} else if (c.equals(Date.class)) {
			cassandraType = "timestamp"
		} else if (c.equals(UUID.class)) {
			cassandraType = "uuid"
		} else if (c.equals(BigInteger.class)) {
			cassandraType = "varint"
		}

		return cassandraType
	}

}
