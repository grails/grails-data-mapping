package org.grails.datastore.gorm.plugin.support

import spock.lang.Specification
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.context.ApplicationContext
import grails.spring.BeanBuilder

/**
 * Tests for Spring configuration
 */
class SpringConfigurerSpec extends Specification{

    void "Test that the ApplicationContext constructed is valid"() {
        when:"An application context is configured"
            ApplicationContext ctx = systemUnderTest()

        then:"The context contains the necessary beans"
            ctx.containsBean("simpleDatastore")
            ctx.containsBean("simpleTransactionManager")
            ctx.containsBean("transactionManager")
            ctx.containsBean("simplePersistenceInterceptor")
            ctx.containsBean("simplePersistenceContextInterceptorAggregator")
            ctx.containsBean("simpleOpenSessionInViewInterceptor")
    }


    ApplicationContext systemUnderTest() {
        def bb = new BeanBuilder()
        bb.beans new SimpleSpringConfigurer().getConfiguration()

        bb.createApplicationContext()
    }
}
class SimpleSpringConfigurer extends SpringConfigurer {
    Map manager = [hasGrailsPlugin:{ String name -> name != 'hibernate' ? true : false }]
    Map application = [:]
    @Override
    String getDatastoreType() {
        return "Simple"
    }

    @Override
    Closure getSpringCustomizer() {
        return {
            simpleDatastore(SimpleMapDatastore)
            simpleMappingContext(KeyValueMappingContext, "test")
        }
    }

}
