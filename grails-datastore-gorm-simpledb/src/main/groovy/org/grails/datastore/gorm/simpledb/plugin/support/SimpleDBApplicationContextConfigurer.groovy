package org.grails.datastore.gorm.simpledb.plugin.support

import org.grails.datastore.gorm.plugin.support.ApplicationContextConfigurer
import org.springframework.context.ConfigurableApplicationContext
import org.grails.datastore.mapping.simpledb.engine.SimpleDBAssociationInfo
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolverFactory
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolver
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst

class SimpleDBApplicationContextConfigurer extends ApplicationContextConfigurer {

    SimpleDBApplicationContextConfigurer() {
        super("SimpleDB")
    }

    @Override
    public void configure(ConfigurableApplicationContext ctx) {
        super.configure(ctx);

        //determine dbCreate flag and create/delete AWS domains if needed
        def simpleDBDomainClasses = []
        simpleDBDomainClassProcessor(application, manager, { dc ->
            simpleDBDomainClasses.add(dc) //collect domain classes which are stored via SimpleDB
        })
        def simpleDBConfig = application.config?.grails?.simpleDB
        handleDBCreate(simpleDBConfig.dbCreate,
                application,
                simpleDBDomainClasses,
                ctx.getBean("simpleDBMappingContext"),
                ctx.getBean("simpleDBDatastore")
        ); //similar to JDBC datastore, do 'create' or 'drop-create'
    }

    /**
     * Iterates over all domain classes which are mapped with SimpleDB and passes them to the specified closure
     */
    def simpleDBDomainClassProcessor = { application, manager, closure ->
        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE || (!isHibernateInstalled && mappedWith == null)) {
                closure.call(dc)
            }
        }
    }

    def handleDBCreate = { dbCreate, application, simpleDBDomainClasses, mappingContext, simpleDBDatastore ->
        boolean drop = false
        boolean create = false
        if ("drop-create" == dbCreate){
            drop = true
            create = true
        } else if ("create" == dbCreate){
            create = true
        }

        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory();
        for (dc in simpleDBDomainClasses) {
            def cls = dc.clazz
            PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName())
            SimpleDBDomainResolver domainResolver = resolverFactory.buildResolver(entity, simpleDBDatastore);
            def domains = domainResolver.getAllDomainsForEntity()
            SimpleDBTemplate template = simpleDBDatastore.getSimpleDBTemplate(entity)

            if (drop){
                domains.each{ domain ->
                    template.deleteDomain (domain)
                }
            }
            if (create){
                domains.each{ domain ->
                    template.createDomain (domain)
                }
            }

            entity.getAssociations().each{ association ->
                //check if this association requires a dedicated aws domain and if yes drop/create if needed
                SimpleDBAssociationInfo associationInfo = simpleDBDatastore.getAssociationInfo(association)
                if (associationInfo){
                    if (drop){
                        template.deleteDomain (associationInfo.getDomainName())
                    }
                    if (create){
                        template.createDomain(associationInfo.getDomainName())
                    }
                }
            }
        }
    }

}


