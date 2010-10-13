package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import spock.lang.Ignore

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 8, 2010
 * Time: 10:22:33 AM
 * To change this template use File | Settings | File Templates.
 */
class ContinuousQuerySpec extends GormDatastoreSpec{


  @Ignore
  void "Test that we receive insert events from a continuous query"() {
    given:
        Plant.cq.findAllByGoesInPatch(true) { event ->
          println "GOT EVENT ${event}"
        }

    when:
      sleep(1000)
      def p = new Plant(name:"cabbage", goesInPatch:true).save()

    then:
      1 == Plant.count()
  }
}
