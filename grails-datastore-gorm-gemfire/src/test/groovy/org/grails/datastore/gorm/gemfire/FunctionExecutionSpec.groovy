package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import spock.lang.Ignore

/**
 * @author graemerocher
 */
class FunctionExecutionSpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [Plant]
    }


    @Ignore
    void "Test a function can be invoked"() {
        given:
            def p = new Plant(name:"cabbage", goesInPatch:true).save()

        when:
            def results = Plant.executeFunction {
                it.lastResult it.localData[p.id]
            }

        then:
            p != null
            results != null
            results.size() == 1
    }

    @Ignore
    void "Test a function can be invoked with a filter"() {
        given:
            def p1 = new Plant(name:"cabbage", goesInPatch:true).save()
            def p2 = new Plant(name:"carrot", goesInPatch:true).save()

        when:
            def results = Plant.executeFunction([p1.id]) {
                it.lastResult it.localData[it.filter.iterator().next()]
            }

        then:
            results != null
            results.size() == 1
            results[0].name == 'cabbage'
    }
}
