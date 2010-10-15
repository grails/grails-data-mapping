package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.Plant
import grails.gorm.tests.GormDatastoreSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 15, 2010
 * Time: 2:13:04 PM
 * To change this template use File | Settings | File Templates.
 */
class FunctionExecutionSpec extends GormDatastoreSpec {

  void "Test a function can be invoked"() {
    given:
      def p = new Plant(name:"cabbage", goesInPatch:true).save()
    when:
      def results = Plant.executeFunction {
        lastResult localData[p.id]
      }

    then:
      p != null
      results != null
      results.size() == 1

  }

  void "Test a function can be invoked with a filter"() {
    given:
      def p1 = new Plant(name:"cabbage", goesInPatch:true).save()
      def p2 = new Plant(name:"carrot", goesInPatch:true).save()
    when:
      def results = Plant.executeFunction([p1.id]) {
        lastResult localData[filter.iterator().next()]
      }

    then:
      results != null
      results.size() == 1
      results[0].name == 'cabbage'
  }
}
