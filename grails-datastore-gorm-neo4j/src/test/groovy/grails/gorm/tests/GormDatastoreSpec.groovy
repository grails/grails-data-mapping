package grails.gorm.tests

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher
import org.grails.datastore.gorm.neo4j.proxy.HashcodeEqualsAwareProxyFactory
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.GrailsDomainClassValidator
import org.neo4j.driver.v1.Driver
import org.neo4j.harness.ServerControls
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.validation.Validator
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
/**
 * Created by graemerocher on 06/06/16.
 */
abstract class GormDatastoreSpec extends Specification {

    List getDomainClasses() {
        [       Book, ChildEntity, City, ClassWithListArgBeforeValidate, ClassWithNoArgBeforeValidate,
                ClassWithOverloadedBeforeValidate, CommonTypes, Country, EnumThing, Face, Highway,
                Location, ModifyPerson, Nose, OptLockNotVersioned, OptLockVersioned, Person, PersonEvent,
                Pet, PetType, Plant, PlantCategory, Publication, Task, TestEntity]
    }

    @Shared @AutoCleanup Neo4jDatastore neo4jDatastore
    @Shared ServerControls serverControls
    @Shared Driver boltDriver
    @Shared GrailsApplication grailsApplication
    @Shared MappingContext mappingContext

    Neo4jSession session

    void setupSpec() {
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        def allClasses = getDomainClasses() as Class[]

        neo4jDatastore = new Neo4jDatastore(
                [(Neo4jDatastore.SETTING_NEO4J_TYPE): Neo4jDatastore.DATABASE_TYPE_EMBEDDED],
                new ConfigurableApplicationContextEventPublisher(ctx),
                allClasses
        )
        serverControls = (ServerControls)Neo4jDatastore.embeddedServer
        boltDriver = neo4jDatastore.boltDriver
        mappingContext = neo4jDatastore.mappingContext

        grailsApplication = new DefaultGrailsApplication(allClasses, getClass().getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()
    }

    void setupValidator(Class entityClass, Validator validator = null) {
        PersistentEntity entity = mappingContext.persistentEntities.find { PersistentEntity e -> e.javaClass == entityClass }
        if (entity) {
            mappingContext.addEntityValidator(entity, validator ?:
                    new GrailsDomainClassValidator(
                            grailsApplication: grailsApplication,
                            domainClass: grailsApplication.getDomainClass(entity.javaClass.name)
                    ) )
        }
    }

    void setup() {
        session = neo4jDatastore.connect()
        DatastoreUtils.bindSession session
        session.beginTransaction()
    }

    void cleanup() {
        session.disconnect()
        DatastoreUtils.unbindSession(session)

        def session = boltDriver.session()
        def tx = session.beginTransaction()
        try {
            tx.run("MATCH (n) DETACH DELETE n")
            tx.success()
        } finally {
            tx.close()
            session.close()
        }
    }

}
