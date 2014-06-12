package org.grails.datastore.gorm.simpledb.plugin.support

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.datastore.gorm.plugin.support.ApplicationContextConfigurer
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore
import org.grails.datastore.mapping.simpledb.config.SimpleDBMappingContext
import org.grails.datastore.mapping.simpledb.engine.SimpleDBAssociationInfo
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolver
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolverFactory
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil
import org.springframework.context.ConfigurableApplicationContext

class SimpleDBApplicationContextConfigurer extends ApplicationContextConfigurer {

    SimpleDBApplicationContextConfigurer() {
        super("SimpleDB")
    }

    @Override
    void configure(ConfigurableApplicationContext ctx) {
        super.configure(ctx)

        GrailsPluginManager pluginManager = ctx.pluginManager
        GrailsApplication application = ctx.grailsApplication

        def simpleDBDomainClasses = []
        simpleDBDomainClassProcessor(application, pluginManager, { dc ->
            simpleDBDomainClasses.add(dc) //collect domain classes which are stored via SimpleDB
        })

        //explicitly register simpledb domain classes with datastore
        SimpleDBDatastore simpleDBDatastore = ctx.simpledbDatastore
        SimpleDBMappingContext mappingContext = ctx.simpledbMappingContext

        simpleDBDomainClasses.each { domainClass ->
            PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.clazz.getName())
            simpleDBDatastore.persistentEntityAdded(entity)
        }

        def simpleDBConfig = application.config?.grails?.simpledb
        //determine dbCreate flag and create/delete AWS domains if needed
        handleDBCreate(simpleDBConfig,
                application,
                simpleDBDomainClasses,
                mappingContext,
                simpleDBDatastore
        ) //similar to JDBC datastore, do 'create' or 'drop-create'
    }

    /**
     * Iterates over all domain classes which are mapped with SimpleDB and passes them to the specified closure
     */
    def simpleDBDomainClassProcessor = { application, pluginManager, closure ->
        def isHibernateInstalled = pluginManager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE || (!isHibernateInstalled && mappedWith == null)) {
                closure.call(dc)
            }
        }
    }

    def handleDBCreate = { simpleDBConfig, application, simpleDBDomainClasses, mappingContext, simpleDBDatastore ->
        String dbCreate = simpleDBConfig.dbCreate
        boolean drop = false
        boolean create = false
        if ("drop-create" == dbCreate) {
            drop = true
            create = true
        } else if ("create" == dbCreate) {
            create = true
        } else if ("drop" == dbCreate) {
            drop = true
        }

        //protection against accidental drop
        boolean disableDrop = simpleDBConfig.disableDrop
        if (disableDrop && drop) {
            throw new IllegalArgumentException("Value of disableDrop is " + disableDrop + " while dbCreate is " +
                dbCreate + ". Throwing an exception to prevent accidental drop of the data")
        }

        def numOfThreads = 30 //how many parallel threads are used to create dbCreate functionality in parallel

        Executor executor = Executors.newFixedThreadPool(numOfThreads)

        SimpleDBTemplate template = simpleDBDatastore.getSimpleDBTemplate()
        List<String> existingDomains = template.listDomains()
        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory()
        CountDownLatch latch = new CountDownLatch(simpleDBDomainClasses.size())

        for (dc in simpleDBDomainClasses) {
            def domainClass = dc.clazz //explicitly declare local variable which we will be using from the thread
            //do simpleDB work in parallel threads for each domain class to speed things up
            executor.execute({
                try {
                    PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.getName())
                    SimpleDBDomainResolver domainResolver = resolverFactory.buildResolver(entity, simpleDBDatastore)
                    def domains = domainResolver.getAllDomainsForEntity()
                    domains.each { domain ->
                        handleDomain(template, existingDomains, domain, drop, create)
                        //handle domains for associations
                        entity.getAssociations().each { association ->
                            SimpleDBAssociationInfo associationInfo = simpleDBDatastore.getAssociationInfo(association)
                            if (associationInfo) {
                                handleDomain(template, existingDomains, associationInfo.getDomainName(), drop, create)
                            }
                        }
                    }
                } finally {
                    latch.countDown()
                }
            })
        }

        //if needed, drop/create hilo id generator domain
        String hiloDomainName = SimpleDBUtil.getPrefixedDomainName(simpleDBDatastore.getDomainNamePrefix(), SimpleDBConst.ID_GENERATOR_HI_LO_DOMAIN_NAME)
        handleDomain(template, existingDomains, hiloDomainName, drop, create)

        latch.await()
        executor.shutdown()
    }

    def clearOrCreateDomain(template, existingDomains, domainName) {
        if (existingDomains.contains(domainName)) {
            template.deleteAllItems(domainName) //delete all items there
        } else {
            //create it
            template.createDomain(domainName)
        }
    }

    def createDomainIfDoesNotExist (template, existingDomains, domainName) {
        if (!existingDomains.contains(domainName)) {
            template.createDomain(domainName)
        }
    }

    def deleteDomainIfExists (template, existingDomains, domainName) {
        if (existingDomains.contains(domainName)) {
            template.deleteDomain(domainName)
        }
    }

    def handleDomain(template, existingDomains, domainName, boolean drop, boolean create) {
        if (drop && create) {
            clearOrCreateDomain(template, existingDomains, domainName)
        } else if (create) {
            createDomainIfDoesNotExist(template, existingDomains, domainName)
        } else if (drop) {
            deleteDomainIfExists(template, existingDomains, domainName)
        }
    }
}
