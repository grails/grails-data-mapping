package org.grails.datastore.gorm

import grails.gorm.tests.Role
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.neo4j.test.ImpermanentGraphDatabase
import org.neo4j.test.TestGraphDatabaseFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.neo4j.rest.graphdb.RestGraphDatabase
import org.neo4j.rest.graphdb.LocalTestServer
import org.neo4j.rest.graphdb.util.Config

class Setup {

    static HOST = "localhost"
    static PORT = 7473
    protected final Logger log = LoggerFactory.getLogger(getClass())

    static datastore
    static transaction
    static server
    static graphDb

    static destroy() {
        transaction.failure()
        transaction.finish()
        graphDb.shutdown()
        server?.stop()
    }

    static Session setup(classes) {

        def ctx = new GenericApplicationContext()
        ctx.refresh()

        if (System.properties.get("gorm_neo4j_test_use_rest")) {
            System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"false") // TODO: remove when support for batch has been finished
            //System.setProperty(Config.CONFIG_LOG_REQUESTS,"true") // enable for verbose request/response logging
            server = new LocalTestServer(HOST, PORT).withPropertiesFile("neo4j-server.properties");
            server.start()
            graphDb = new RestGraphDatabase("http://localhost:7473/db/data/")
        } else {
            graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
        }

        datastore = new Neo4jDatastore(graphDatabaseService: graphDb, applicationContext: ctx)
        datastore.mappingContext.proxyFactory = new GroovyProxyFactory()

        /*Neo4jSession.metaClass.invokeMethod = { String name, args ->
            def metaMethod = Neo4jSession.metaClass.getMetaMethod(name, args)
            if (metaMethod==null) {
                metaMethod = Neo4jSession.metaClass.methods.find {it.name==name}
            }
            log.warn "START $name ($args)"
            try {
                metaMethod.invoke(delegate, args)

            } finally {
                log.warn "DONE $name"
            }
        }*/

        for (cls in classes) {
            datastore.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = datastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        datastore.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        entity = datastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("Role")}
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
        enhancer.enhance()

        datastore.afterPropertiesSet()

        datastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        transaction = graphDb.beginTx()
        datastore.connect()
    }
}
