package org.grails.datastore.gorm

import java.nio.ByteBuffer
import org.grails.datastore.gorm.cassandra.CassandraGormEnhancer
import org.grails.datastore.gorm.cassandra.CassandraMethodsConfigurer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.CassandraMappingContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datstore.gorm.cassandra.CassandraMethodsConfigurer
import org.springframework.context.support.GenericApplicationContext


class Setup {

	static Session session
	static CassandraDatastore ds

	static destroy() {
		ds.destroy()
	}

	static boolean catchException(def block) {
		boolean caught = false;
		try {
			block()
		}
		catch (Exception e) {
			caught = true
		}
		return caught
	}

	static Session setup(List<Class> classes) {
		def ctx = new GenericApplicationContext()
		ctx.refresh()

		ConfigObject config = new ConfigObject()
		config.put("contactPoints", "jeff-cassandra.dev.wh.reachlocal.com")
		ds = new CassandraDatastore(new CassandraMappingContext(CassandraDatastore.DEFAULT_KEYSPACE), ctx, config)
		//		ds.applicationContext = ctx

		def entities = []
		for (cls in classes) {
			entities << ds.mappingContext.addPersistentEntity(cls)
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

		session = ds.connect()


		def nativeSession = session.getNativeInterface()
		catchException {
			nativeSession.execute("""
                CREATE KEYSPACE ${ds.DEFAULT_KEYSPACE} WITH replication = {
                    'class': 'SimpleStrategy',
                    'replication_factor': '1'
                };
                """)
		}
		// Get persistent entities again after they have been enhanced
		entities.each { entity ->
			createOrCleanTable(entity, ds)
		}

		return session
	}

	private static void createOrCleanTable(PersistentEntity entity, CassandraDatastore ds) {
		println "Configuring $entity"
		def nativeSession = session.getNativeInterface()
		def tableName = entity.getDecapitalizedName()
		def keyspace = ds.getMappingContext().getKeyspace()

		//String dropTable = "DROP TABLE IF EXISTS ${ds.DEFAULT_KEYSPACE}.${tableName};"
		String dropTable = "DROP TABLE ${keyspace}.${tableName};"
		String truncateTable = "TRUNCATE ${keyspace}.${tableName};"

		//String createTable = "CREATE TABLE IF NOT EXISTS ${ds.DEFAULT_KEYSPACE}.${tableName} ("
		String createTable = "CREATE TABLE ${keyspace}.${tableName} ("
		def createIndices = []
		def id = entity.identity
		createTable += id.getMapping().getMappedForm().getTargetName() + " " + getCassandraType(id.getType()) + " PRIMARY KEY,"
		entity.getPersistentProperties()
		List props = entity.persistentProperties.collect { prop ->
			String propName = prop.getMapping().getMappedForm().getTargetName()
			String propDef = null
			if (prop instanceof ToOne) {
				ToOne toOne = (ToOne)prop
				propDef = "${propName} ${getCassandraType(toOne.associatedEntity.identity.getType())}"
			} else if (prop instanceof OneToMany || prop instanceof ManyToMany) {} else {
				propDef = "${propName} ${getCassandraType(prop.getType())}"
			}
			if (prop.mapping.mappedForm.isIndex()) {
				createIndices << "CREATE INDEX on ${keyspace}.${tableName} (${propName});"
			}
			return propDef
		}
		createTable += props.findAll().join(",") + ");"

		catchException { nativeSession.execute(truncateTable); println truncateTable; }
		//catchException { nativeSession.execute(dropTable); println dropTable }
		catchException { nativeSession.execute(createTable); println createTable }
		createIndices.each { createIndex -> catchException { nativeSession.execute(createIndex); println createIndex; } }

	}

	private static String getCassandraType(Class c) {

		String cassandraType = "blob"

		if (c.is(String.class)) {
			cassandraType = "text"
		} else if (c.equals(long.class) || c.equals(Long.class)) {
			cassandraType = "bigint"
		} else if (c.equals(ByteBuffer.class)) {
			cassandraType = "blob"
		} else if (c.equals(boolean.class) || c.equals(Boolean.class)) {
			cassandraType = "boolean"
		} else if (c.equals(BigDecimal.class)) {
			cassandraType = "decimal"
		} else if (c.equals(double.class)) {
			cassandraType = "double"
		} else if (c.equals(float.class) || c.equals(Float.class)) {
			cassandraType = "float"
		} else if (c.equals(int.class) || c.equals(Integer.class) || c.equals(short.class) || c.equals(Short.class) || c.equals(byte.class) || c.equals(Byte.class)) {
			cassandraType = "int"
		} else if (c.equals(List.class)) {
			cassandraType = "list<text>"
		} else if (c.equals(Map.class)) {
			cassandraType = "map<text,text>"
		} else if (c.equals(Set.class)) {
			//TODO reflection to find generic type? Should sets vs hasmany be handled a certian way
			cassandraType = "set<text>"
		} else if (c.equals(String.class) || c.equals(URL.class) || c.equals(TimeZone.class) || c.equals(Locale.class) || c.equals(Currency.class)) {
			cassandraType = "text"
		} else if (c.equals(Date.class) || (Calendar.class.isAssignableFrom(c))) {
			cassandraType = "timestamp"
		} else if (c.equals(UUID.class)) {
			cassandraType = "uuid"
		} else if (c.equals(BigInteger.class)) {
			cassandraType = "varint"
		}

		return cassandraType
	}

}
