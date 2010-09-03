package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 3, 2010
 * Time: 11:19:36 AM
 * To change this template use File | Settings | File Templates.
 */
class GroovyProxySpec extends GormDatastoreSpec{

  void "Test creation and behavior of Groovy proxies"() {

    given:
      session.mappingContext.proxyFactory = new org.grails.datastore.gorm.proxy.GroovyProxyFactory()
      def id = new Location(name:"United Kingdom", code:"UK").save(flush:true)?.id
      session.clear()


    when:
      def location = Location.proxy(id)

    then:

      location != null
      false == location.isInitialized()
      false == location.initialized
      null == location.target

      "UK" == location.code
      "United Kingdom - UK" == location.namedAndCode()
      true == location.isInitialized()
      true == location.initialized
      null != location.target


  }
}
