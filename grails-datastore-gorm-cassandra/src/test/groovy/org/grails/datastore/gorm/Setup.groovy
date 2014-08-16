package org.grails.datastore.gorm

import org.grails.datastore.gorm.cassandra.plugin.support.CassandraMethodsConfigurer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import com.datastax.driver.core.KeyspaceMetadata


class Setup {

    static CassandraDatastore cassandraDatastore
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

    static Session setup(List<Class> classes) {        
        if (cassandraDatastore == null) {
            def ctx = new GenericApplicationContext()
            ctx.refresh()

            ConfigObject config = new ConfigObject()            
            config.setProperty(CassandraDatastore.CASSANDRA_SCHEMA_ACTION, "RECREATE_DROP_UNUSED")
            //config.setProperty(CassandraDatastore.CASSANDRA_CONTACT_POINTS, "localhost")
            cassandraDatastore = new CassandraDatastore(new CassandraMappingContext("unittest"), config, ctx)
            cassandraDatastore.createCluster()

            ensureKeyspace()

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
        }
        
        def txMgr = new DatastoreTransactionManager(datastore: cassandraDatastore)
        CassandraMethodsConfigurer methodsConfigurer = new CassandraMethodsConfigurer(cassandraDatastore, txMgr)
        methodsConfigurer.configure()
        
        def cassandraSession = cassandraDatastore.connect()
        
        deleteAllEntities(cassandraSession)

        return cassandraSession
    }

    private static void ensureKeyspace() {
        def cluster = cassandraDatastore.getNativeCluster()

        if (!StringUtils.hasText(keyspace)) {
            keyspace = null
        }

        if (keyspace != null) {
            // see if we need to create the keyspace
            KeyspaceMetadata kmd = cluster.getMetadata().getKeyspace(keyspace)
            if (kmd == null) { // then create keyspace

                def cql = "CREATE KEYSPACE " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};"
                println "creating keyspace ${keyspace} via CQL [${cql}]"
                def session = cluster.connect()
                session.execute(cql)
                session.close()
            }
        }
    }

    public static void deleteAllEntities(def cassandraSession) {
        if (!cassandraSession.cassandraTemplate) {
            throw new RuntimeException("Cassandra Template not found, possible reason: Spring Data Cassandra not initialized")
        }
        for (CassandraPersistentEntity<?> entity : cassandraSession.cassandraTemplate?.cassandraConverter?.mappingContext?.persistentEntities) {
            cassandraSession.cassandraTemplate.truncate(entity.getTableName())
        }
    }

}
