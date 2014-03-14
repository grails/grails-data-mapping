package org.grails.datastore.gorm.mongo.plugin.support

import com.gmongo.GMongo
import org.codehaus.groovy.grails.commons.GrailsApplication
import spock.lang.Specification
import grails.spring.BeanBuilder
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import grails.gorm.tests.Person
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

/**
 */
class MongoSpringConfigurerSpec extends Specification{

    void "Test configure mongo via Spring for connection string"() {
        when:"The spring configurer is used"
            def configurer = new MongoSpringConfigurer()
            def config = configurer.getConfiguration()
            def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)


            def connectionString = "mongodb://localhost/mydb"
            application.config.grails.mongo.connectionString = connectionString
            BeanBuilder bb = createInitialApplicationContext(application)
            bb.beans config

        then:"Then the mongo bean has its connection string set"
            bb.getBeanDefinition('mongo').getPropertyValues().getPropertyValue('connectionString').value == connectionString
    }

    void "Test configure Mongo via Spring"() {
        when:"The spring configurer is used"
            def configurer = new MongoSpringConfigurer()
            def config = configurer.getConfiguration()
            final defaultConfig = {
                '*'(reference: true)
            }
            def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)
            BeanBuilder bb = createInitialApplicationContext(application, defaultConfig)
            bb.beans config
            def ctx = bb.createApplicationContext()
            MongoMappingContext mappingContext = ctx.getBean("mongoMappingContext",MongoMappingContext)
            def entity = mappingContext?.getPersistentEntity(Person.name)

        then:"The application context is created"
            ctx != null
            ctx.containsBean("persistenceInterceptor")
            mappingContext.defaultMapping == defaultConfig
            entity != null
            entity.getPropertyByName('pets').getMapping().mappedForm.reference == true

    }

    protected BeanBuilder createInitialApplicationContext(GrailsApplication application, Closure defaultMappingConfig = {}) {
        def bb = new BeanBuilder()
        final binding = new Binding()


        application.config.grails.mongo.databaseName = "test"
        application.config.grails.mongo.default.mapping = defaultMappingConfig
        application.initialise()
        application.registerArtefactHandler(new DomainClassArtefactHandler())
        binding.setVariable("application", application)
        binding.setVariable("manager", new DefaultGrailsPluginManager([] as Class[], application))
        bb.setBinding(binding)
        bb.beans {
            grailsApplication(DefaultGrailsApplication, [Person] as Class[], Thread.currentThread().contextClassLoader) { bean ->
                bean.initMethod = "initialise"
            }
            pluginManager(DefaultGrailsPluginManager, [] as Class[], ref("grailsApplication"))
        }

        return bb
    }
}
