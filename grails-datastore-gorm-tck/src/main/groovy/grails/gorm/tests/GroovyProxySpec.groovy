package grails.gorm.tests

import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.springframework.dao.DataIntegrityViolationException

/**
 * @author graemerocher
 */
class GroovyProxySpec extends GormDatastoreSpec {

    void "Test proxying of non-existent instance throws an exception"() {
        given:"A groovy proxy factory"
            session.mappingContext.proxyFactory = new GroovyProxyFactory()

        when:"A proxy is loaded for an instance that doesn't exist"
            def location = Location.proxy(123)

        then:"The proxy is in a valid state"

            location != null
            123 == location.id
            false == location.isInitialized()
            false == location.initialized

        when:"The proxy is loaded"
            location.code

        then:"An exception is thrown"
            thrown DataIntegrityViolationException
    }

    void "Test creation and behavior of Groovy proxies"() {

        given:
            session.mappingContext.proxyFactory = new GroovyProxyFactory()
            def id = new Location(name:"United Kingdom", code:"UK").save(flush:true)?.id
            session.clear()

        when:
            def location = Location.proxy(id)

        then:

            location != null
            id == location.id
            Location == location.class
            null != location.metaClass
            false == location.isInitialized()
            false == location.initialized

            "UK" == location.code
            "United Kingdom - UK" == location.namedAndCode()
            true == location.isInitialized()
            true == location.initialized
            null != location.target
            Location == location.class
            null != location.metaClass
    }

    void "Test setting metaClass property on proxy"() {
        given:
            session.mappingContext.proxyFactory = new GroovyProxyFactory()
        when:
            def location = Location.proxy(123)
            location.metaClass = null
        then:
            location.metaClass != null
    }

    void "Test calling setMetaClass method on proxy"() {
        given:
            session.mappingContext.proxyFactory = new GroovyProxyFactory()
        when:
            def location = Location.proxy(123)
            location.setMetaClass(null)
        then:
            location.metaClass != null
    }
        
    void "Test creation and behavior of Groovy proxies with method call"() {

        given:
        session.mappingContext.proxyFactory = new GroovyProxyFactory()
        def id = new Location(name:"United Kingdom", code:"UK").save(flush:true)?.id
        session.clear()

        when:
        def location = Location.proxy(id)

        then:

        location != null
        id == location.id
        Location == location.class
        null != location.metaClass
        false == location.isInitialized()
        false == location.initialized

        "United Kingdom - UK" == location.namedAndCode() // method first
        "UK" == location.code
        true == location.isInitialized()
        true == location.initialized
        null != location.target
        Location == location.class
        null != location.metaClass
    }
}
