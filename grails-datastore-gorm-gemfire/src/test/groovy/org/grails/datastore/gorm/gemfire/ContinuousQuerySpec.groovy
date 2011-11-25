package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import spock.lang.Ignore

/**
 * @author graemerocher
 */
class ContinuousQuerySpec extends GormDatastoreSpec {

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
