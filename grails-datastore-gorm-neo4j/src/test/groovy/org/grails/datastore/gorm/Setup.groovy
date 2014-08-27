package org.grails.datastore.gorm

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.DumpGraphOnSessionFlushListener
import org.grails.datastore.gorm.neo4j.HashcodeEqualsAwareProxyFactory
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.neo4j.Neo4jMappingContext
import org.grails.datastore.gorm.neo4j.TestServer
import org.grails.datastore.gorm.neo4j.engine.JdbcCypherEngine
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.validation.GrailsDomainClassValidator
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.kernel.impl.transaction.TxManager
import org.neo4j.server.web.WebServer
import org.neo4j.test.TestGraphDatabaseFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

class Setup {

    protected static final Logger log = LoggerFactory.getLogger(getClass())

    static Neo4jDatastore datastore
    static GraphDatabaseService graphDb
    static DataSource dataSource
    static WebServer webServer
    static skipIndexSetup = true
    static Closure extendedValidatorSetup = null

    static destroy() {
        dataSource.close()
        TxManager txManager = ((GraphDatabaseAPI)graphDb).getDependencyResolver().resolveDependency(TxManager)
        log.info "before shutdown, active: $txManager.activeTxCount, committed $txManager.committedTxCount, started: $txManager.startedTxCount, rollback: $txManager.rolledbackTxCount, status: $txManager.status"
        assert txManager.activeTxCount == 0, "something is wrong with connection handling - we still have $txManager.activeTxCount connections open"

        webServer?.stop()
        graphDb?.shutdown()
        log.info "after shutdown"
    }

    static Session setup(classes) {

        def ctx = new GenericApplicationContext()
        ctx.refresh()

        MappingContext mappingContext = new Neo4jMappingContext()

        // setup datasource
        def testMode = System.properties.get("gorm_neo4j_test_mode", "embedded")

        def poolprops = [
                driverClassName: 'org.neo4j.jdbc.Driver',
                defaultAutoCommit: false,
        ]

        switch (testMode) {
            case "embedded":
                graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .setConfig("cache_type", "soft") // prevent hpc cache during tests, potentially leaking memory due to many restarts
                        .newGraphDatabase()
                def instanceName = ManagementFactory.runtimeMXBean.name
                poolprops.url = "jdbc:neo4j:instance:${instanceName}"
                        //url: 'jdbc:neo4j:mem',
                poolprops.dbProperties = ["$instanceName": graphDb]
                break
            case "server":
                graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
                def port
                (port, webServer) = TestServer.startWebServer(graphDb)
                poolprops.url = "jdbc:neo4j://localhost:$port/"
                break

            case "remote":
                poolprops.url = "jdbc:neo4j://localhost:7474/"
                break
            default:
                throw new IllegalStateException("dunno know how to handle mode $testMode")
        }
        dataSource = new DataSource(new PoolProperties(poolprops))

        datastore = new Neo4jDatastore(
                mappingContext,
                ctx,
                new JdbcCypherEngine(dataSource)
        )
        datastore.skipIndexSetup = skipIndexSetup
        datastore.mappingContext.proxyFactory = new HashcodeEqualsAwareProxyFactory()


//        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, UniqueConstraint)
        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }

        def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()

        setupValidators(mappingContext, grailsApplication)

        def enhancer = new Neo4jGormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
        enhancer.enhance()

        datastore.afterPropertiesSet()

        waitForIndexesBeingOnline(graphDb)

        mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)

      // enable for debugging
        if (graphDb) {
            ctx.addApplicationListener new DumpGraphOnSessionFlushListener(graphDb)
        }

        datastore.connect()
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

    private static void waitForIndexesBeingOnline(GraphDatabaseService graphDb) {
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
