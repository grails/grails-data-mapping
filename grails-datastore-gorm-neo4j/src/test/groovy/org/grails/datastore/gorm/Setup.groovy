package org.grails.datastore.gorm

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.util.Holders
import org.grails.core.lifecycle.ShutdownOperations
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.proxy.HashcodeEqualsAwareProxyFactory
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jDatastoreTransactionManager
import org.grails.datastore.gorm.neo4j.Neo4jMappingContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.GrailsDomainClassValidator
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders
import org.neo4j.harness.internal.Ports
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.util.concurrent.TimeUnit

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.BoltConnector.EncryptionLevel.DISABLED
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnector

class Setup {

    protected static final Logger log = LoggerFactory.getLogger(getClass())

    static Neo4jDatastore datastore
    static ServerControls serverControls
    static skipIndexSetup = true
    static Closure extendedValidatorSetup = null

    static destroy() {
        def enhancer = new GormEnhancer(datastore, new Neo4jDatastoreTransactionManager(datastore: datastore))
        enhancer.close()
        serverControls?.close()
        serverControls = null
        datastore.close()
        datastore = null

        // force clearing of thread locals, Neo4j connection pool leaks :(
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet()
        for(t in threadSet) {
            try {
                def f = Thread.getDeclaredField("threadLocals")
                f.accessible = true
                f.set(t, null)
            } catch (Throwable e) {
                println "ERROR: Cannot clear thread local $e.message"
            }
        }

        ShutdownOperations.runOperations()
        Holders.clear()
        log.info "after shutdown"
    }

    static Session setup(classes) {
        System.setProperty("neo4j.gorm.suite", "true")

        assert datastore == null
        def ctx = new GenericApplicationContext()
        ctx.refresh()

        boolean nativeId = Boolean.getBoolean("gorm.neo4j.test.nativeId")

        def nativeIdMapping = {
            id generator:'native'
        }
        MappingContext mappingContext = nativeId ? new Neo4jMappingContext(nativeIdMapping) : new Neo4jMappingContext()

        // setup datasource

        InetSocketAddress inetAddr = Ports.findFreePort("localhost", [ 7687, 64 * 1024 - 1 ] as int[])
        String myBoltAddress = String.format("%s:%d", inetAddr.getHostName(), inetAddr.getPort())

        serverControls = TestServerBuilders.newInProcessBuilder()
                .withConfig(boltConnector("0").enabled, "true")
                .withConfig(boltConnector("0").encryption_level, DISABLED.name())
                .withConfig(boltConnector("0").address, myBoltAddress)
                .newServer()

        Driver boltDriver = GraphDatabase.driver("bolt://" + myBoltAddress, Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())

        datastore = new Neo4jDatastore(
                boltDriver,
                mappingContext,
                ctx
        )
        datastore.skipIndexSetup = skipIndexSetup
        datastore.mappingContext.proxyFactory = new HashcodeEqualsAwareProxyFactory()


        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }

        def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()

        setupValidators(mappingContext, grailsApplication)


        def transactionManager = new Neo4jDatastoreTransactionManager(datastore: datastore)
        def enhancer = new GormEnhancer(datastore, transactionManager)
        enhancer.enhance()


//        waitForIndexesBeingOnline(graphDb)

        mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)

        def session = datastore.connect()
        session.beginTransaction()
        return session
    }

    static void setupValidators(MappingContext mappingContext, GrailsApplication grailsApplication) {

        setupValidator(mappingContext, grailsApplication, "TestEntity", [
                    supports: { Class c -> true },
                    validate: { o, Errors errors ->
                        if (!StringUtils.hasText(o.name)) {
                            errors.rejectValue("name", "name.is.blank")
                        }
                    }
                ] as Validator)

        setupValidator(mappingContext, grailsApplication, "Role")

        if (extendedValidatorSetup) {
            extendedValidatorSetup(mappingContext, grailsApplication)
        }
    }

    static void setupValidator(MappingContext mappingContext, GrailsApplication grailsApplication, String entityName, Validator validator = null) {
        PersistentEntity entity = mappingContext.persistentEntities.find { PersistentEntity e -> e.javaClass.simpleName == entityName }
        if (entity) {
            mappingContext.addEntityValidator(entity, validator ?:
                    new GrailsDomainClassValidator(
                            grailsApplication: grailsApplication,
                            domainClass: grailsApplication.getDomainClass(entity.javaClass.name)
                    ) )
        }
    }

    private static void waitForIndexesBgeingOnline(GraphDatabaseService graphDb) {
        if (graphDb && (skipIndexSetup==false)) {
            def tx = graphDb.beginTx()
            try {
                graphDb.schema().awaitIndexesOnline(10, TimeUnit.SECONDS)
                tx.success()
            } finally {
                tx.close()
            }
        }
    }

}
