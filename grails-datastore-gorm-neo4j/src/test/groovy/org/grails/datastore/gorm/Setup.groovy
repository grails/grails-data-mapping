package org.grails.datastore.gorm

import grails.gorm.tests.Role
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.DumpGraphOnSessionFlushListener
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.neo4j.Neo4jMappingContext
import org.grails.datastore.gorm.neo4j.engine.EmbeddedCypherEngine
import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Transaction
import org.neo4j.test.TestGraphDatabaseFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class Setup {

    static HOST = "localhost"
    static PORT = 7473
    protected final Logger log = LoggerFactory.getLogger(getClass())

    static Neo4jDatastore datastore
    static GraphDatabaseService graphDb

    static destroy() {
        graphDb.shutdown()
        //server?.stop()
    }

    static Session setup(classes) {

        def ctx = new GenericApplicationContext()
        ctx.refresh()

        initializeGraphDatabaseSerivce()

        MappingContext mappingContext = new Neo4jMappingContext()

        datastore = new Neo4jDatastore(
                mappingContext,
                ctx,
                new EmbeddedCypherEngine(graphDb)
        )
        datastore.skipIndexSetup = true
        //datastore.mappingContext.proxyFactory = new GroovyProxyFactory()

        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        entity = mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("Role")}
        if (entity) {

            def grailsApplication = new DefaultGrailsApplication([Role] as Class[], Setup.getClassLoader())
            grailsApplication.mainContext = ctx
            grailsApplication.initialise()

            def validator = new GrailsDomainClassValidator(
                grailsApplication: grailsApplication,
                domainClass: grailsApplication.getDomainClass(entity.name)
            )

            datastore.mappingContext.addEntityValidator(entity, validator)
        }

        def enhancer = new Neo4jGormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
//        def enhancer = new GormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
        enhancer.enhance()

        datastore.afterPropertiesSet()

        mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)
        ctx.addApplicationListener new DumpGraphOnSessionFlushListener(graphDb)

        datastore.connect()
    }

    private static initializeGraphDatabaseSerivce() {
        if (System.properties.get("gorm_neo4j_test_use_rest")) {
//            System.setProperty(Config.CONFIG_BATCH_TRANSACTION, "false") // TODO: remove when support for batch has been finished
            //System.setProperty(Config.CONFIG_LOG_REQUESTS,"true") // enable for verbose request/response logging
            //server = new LocalTestServer(HOST, PORT).withPropertiesFile("neo4j-server.properties");
            //server.start()
            //graphDb = new RestGraphDatabase("http://localhost:7473/db/data/")
        } else {
            graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
        }
    }
}
