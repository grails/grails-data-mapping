package grails.gorm.tests

import spock.lang.Ignore;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 3, 2010
 * Time: 11:19:36 AM
 * To change this template use File | Settings | File Templates.
 */
class GroovyProxySpec extends GormDatastoreSpec{

  @Ignore
  void "Test creation and behavior of Groovy proxies"() {

	given:
	  session.mappingContext.proxyFactory = new org.grails.datastore.gorm.proxy.GroovyProxyFactory()
	  def id = new Location(name:"United Kingdom", code:"UK").save(flush:true)?.id
	  session.clear()


	when:
	  def location = Location.proxy(id)

	then:

	  location != null
	  id == location.id
	  false == location.isInitialized()
	  false == location.initialized

	  "UK" == location.code
	  "United Kingdom - UK" == location.namedAndCode()
	  true == location.isInitialized()
	  true == location.initialized
	  null != location.target


  }
}
