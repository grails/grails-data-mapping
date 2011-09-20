package org.grails.datastore.gorm

import grails.gorm.tests.Role
import grails.gorm.tests.User

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.neo4j.constraints.UniqueConstraint
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import grails.gorm.tests.Tournament
import grails.gorm.tests.Team
import grails.gorm.tests.Club

class Setup {

    protected final Logger log = LoggerFactory.getLogger(getClass())


    static session
    static datastore
    static transaction
    static storeDir

    static destroy() {
        transaction.rollback()
        session.nativeInterface.shutdown()
        new File(storeDir).deleteDir()
    }

    static Session setup(classes) {

        storeDir = File.createTempFile("neo4j",null)
        assert storeDir.delete()
        assert storeDir.mkdir()
        storeDir = storeDir.path
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        datastore = new Neo4jDatastore(storeDir: storeDir, applicationContext: ctx)
        datastore.afterPropertiesSet()

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

        classes << User << Role << Tournament << Team << Club
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, UniqueConstraint)

        /*def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        grailsApplication.mainContext = new GenericApplicationContext()
        grailsApplication.initialise()


        grailsApplication.mainContext.refresh()
        datastore.applicationContext = grailsApplication.mainContext // grailsApplication.mainContext*/


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

        def grailsApplication = new DefaultGrailsApplication([Role] as Class[], Setup.getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()

        def validator = new GrailsDomainClassValidator(
            grailsApplication: grailsApplication,
            domainClass: grailsApplication.getDomainClass(entity.name)
        )

        datastore.mappingContext.addEntityValidator(entity, validator)

        def enhancer = new Neo4jGormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
        enhancer.enhance()

        datastore.afterPropertiesSet()

        datastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        session = datastore.connect()
        transaction = session.beginTransaction()
        session
    }
}
