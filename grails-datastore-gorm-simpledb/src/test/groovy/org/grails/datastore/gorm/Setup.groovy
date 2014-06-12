package org.grails.datastore.gorm

import grails.gorm.tests.PlantNumericIdValue

import java.util.concurrent.CountDownLatch

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.simpledb.SimpleDBGormEnhancer
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore
import org.grails.datastore.mapping.simpledb.config.SimpleDBMappingContext
import org.grails.datastore.mapping.simpledb.engine.SimpleDBAssociationInfo
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolver
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolverFactory
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * In order to run AWS SimpleDB tests you have to define two system variables: AWS_ACCESS_KEY and AWS_SECRET_KEY with
 * your aws credentials and then invoke this command from main directory:
 * gradlew grails-datastore-gorm-simpledb:test
 *
 * or this one to run one specific test:
 * gradlew -Dtest.single=CrudOperationsSpec grails-datastore-gorm-simpledb:test
 *
 * @author graemerocher
 * @author Roman Stepanenko
 */
class Setup {

    static simpleDB
    static session

    static destroy() {
        session.nativeInterface.dropDatabase()
    }

    static Session setup(classes) {
        classes.add(PlantNumericIdValue.class)

        def env = System.getenv()
        final userHome = System.getProperty("user.home")
        def settingsFile = new File(userHome, "aws.properties")
        def connectionDetails = [:]
        if (settingsFile.exists()) {
            def props = new Properties()
            settingsFile.withReader { reader ->
                props.load(reader)
            }
            connectionDetails.put(SimpleDBDatastore.ACCESS_KEY, props['AWS_ACCESS_KEY'])
            connectionDetails.put(SimpleDBDatastore.SECRET_KEY, props['AWS_SECRET_KEY'])
        }

        connectionDetails.put(SimpleDBDatastore.DOMAIN_PREFIX_KEY, "TEST_")
        connectionDetails.put(SimpleDBDatastore.DELAY_AFTER_WRITES_MS, "7000") //this flag will cause pausing for that many MS after each write - to fight eventual consistency

        simpleDB = new SimpleDBDatastore(new SimpleDBMappingContext(), connectionDetails)
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        simpleDB.applicationContext = ctx
        simpleDB.afterPropertiesSet()

        for (cls in classes) {
            simpleDB.mappingContext.addPersistentEntity(cls)
        }

        cleanOrCreateDomainsIfNeeded(classes, simpleDB.mappingContext, simpleDB)

        PersistentEntity entity = simpleDB.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        simpleDB.mappingContext.addEntityValidator(entity, [
                supports: { Class c -> true },
                validate: { Object o, Errors errors ->
                    if (!StringUtils.hasText(o.name)) {
                        errors.rejectValue("name", "name.is.blank")
                    }
                }
        ] as Validator)

        def enhancer = new SimpleDBGormEnhancer(simpleDB, new DatastoreTransactionManager(datastore: simpleDB))
        enhancer.enhance()

        simpleDB.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        simpleDB.applicationContext.addApplicationListener new DomainEventListener(simpleDB)
        simpleDB.applicationContext.addApplicationListener new AutoTimestampEventListener(simpleDB)

        session = simpleDB.connect()

        return session
    }

    /**
     * Creates AWS domain if AWS domain corresponding to a test entity class does not exist, or cleans it if it does exist.
     * @param domainClasses
     * @param mappingContext
     * @param simpleDBDatastore
     */
    static void cleanOrCreateDomainsIfNeeded(def domainClasses, mappingContext, simpleDBDatastore) {
        SimpleDBTemplate template = simpleDBDatastore.getSimpleDBTemplate()
        List<String> existingDomains = template.listDomains()
        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory()
        CountDownLatch latch = new CountDownLatch(domainClasses.size())
        for (dc in domainClasses) {
            def domainClass = dc //explicitly declare local variable which we will be using from the thread
            //do simpleDB work in parallel threads for each domain class to speed things up
            Thread.start {
                try {
                    PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.getName())
                    SimpleDBDomainResolver domainResolver = resolverFactory.buildResolver(entity, simpleDBDatastore)
                    def domains = domainResolver.getAllDomainsForEntity()
                    domains.each { domain ->
                        clearOrCreateDomain(template, existingDomains, domain)
                        //create domains for associations
                        entity.getAssociations().each{ association ->
                            SimpleDBAssociationInfo associationInfo = simpleDBDatastore.getAssociationInfo(association)
                            if (associationInfo) {
                                clearOrCreateDomain(template, existingDomains, associationInfo.getDomainName())
                            }
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
    }

    static clearOrCreateDomain(template, existingDomains, domainName) {
        if (existingDomains.contains(domainName)) {
            template.deleteAllItems(domainName) //delete all items there
        } else {
            //create it
            template.createDomain(domainName)
        }
    }
}
