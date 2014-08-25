package org.grails.datastore.gorm

import org.grails.datastore.gorm.cassandra.plugin.support.CassandraMethodsConfigurer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.grails.datastore.mapping.cassandra.config.CassandraPersistentEntity
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator


class Setup {

    static CassandraDatastore cassandraDatastore
	static CassandraTemplate cassandraTemplate
	static CassandraAdminTemplate cassandraAdminTemplate
    static String keyspace = "unittest"

    static destroy() {
    }

    static boolean catchException(def block) {
        boolean caught = false
        try {
            block()
        }
        catch (Exception e) {
            caught = true
        }
        return caught
    }

    static Session setup(List<Class> classes, List<Class> additionalClasses) {        
		// need to split out cluster creation, keyspace creation, persistent entity addition and session creation 
		// in order to create nativeSession and schema's in the order defined by Spring Data Cassandra
        if (cassandraDatastore == null) {
            def ctx = new GenericApplicationContext()
            ctx.refresh()

            ConfigObject config = new ConfigObject()            
            config.setProperty(CassandraDatastore.CASSANDRA_SCHEMA_ACTION, "RECREATE_DROP_UNUSED")
            //config.setProperty(CassandraDatastore.CASSANDRA_CONTACT_POINTS, "localhost")
            def cassandraDatastore = new CassandraDatastore(new CassandraMappingContext("unittest"), config, ctx)
            def nativeCluster = cassandraDatastore.createCluster()
			if (!nativeCluster) {
				throw new RuntimeException("Cassandra Template not found, possible reason: Spring Data Cassandra not initialized")
			}

            ensureKeyspace(nativeCluster)

            def entities = []
            for (cls in classes) {
                entities << cassandraDatastore.mappingContext.addPersistentEntity(cls)
            }

            PersistentEntity entity = cassandraDatastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

            cassandraDatastore.mappingContext.addEntityValidator(entity, [
                supports: { Class c -> true },
                validate: { Object o, Errors errors ->
                    if (!StringUtils.hasText(o.name)) {
                        errors.rejectValue("name", "name.is.blank")
                    }
                }
            ] as Validator)
                                                                
            cassandraDatastore.applicationContext.addApplicationListener new DomainEventListener(cassandraDatastore)
            cassandraDatastore.applicationContext.addApplicationListener new AutoTimestampEventListener(cassandraDatastore)
            
            cassandraDatastore.afterPropertiesSet()
			
			this.cassandraDatastore = cassandraDatastore
			cassandraTemplate = cassandraDatastore.cassandraTemplate
			if (!cassandraTemplate) {
				throw new RuntimeException("Cassandra Template not found, possible reason: Spring Data Cassandra not initialized")
			}
			cassandraAdminTemplate = new CassandraAdminTemplate(cassandraDatastore.nativeSession, cassandraDatastore.cassandraTemplate.cassandraConverter)
        }
		
		for (cls in additionalClasses) {
			if (!cassandraDatastore.mappingContext.isPersistentEntity(cls)) {
				cassandraDatastore.mappingContext.addPersistentEntity(cls)
				cassandraDatastore.createTableDefinition(cls)				
			}
		}
		
        def txMgr = new DatastoreTransactionManager(datastore: cassandraDatastore)
        CassandraMethodsConfigurer methodsConfigurer = new CassandraMethodsConfigurer(cassandraDatastore, txMgr)
        methodsConfigurer.configure()
        
        def cassandraSession = cassandraDatastore.connect()     
        truncateAllEntities()
               
        return cassandraSession
    }

    private static void ensureKeyspace(nativeCluster) {        
        if (!StringUtils.hasText(keyspace)) {
            keyspace = null
        }

        if (keyspace != null) {           
            def cql = "CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};"
            println "creating keyspace ${keyspace} via CQL [${cql}]"
            def session = nativeCluster.connect()
            session.execute(cql)
            session.close()
        }        
    }

    public static void truncateAllEntities() {        
        for (CassandraPersistentEntity entity : cassandraDatastore.mappingContext.persistentEntities) {
            cassandraTemplate.truncate(cassandraTemplate.getTableName(entity.getJavaClass()))
        }
    }

}
