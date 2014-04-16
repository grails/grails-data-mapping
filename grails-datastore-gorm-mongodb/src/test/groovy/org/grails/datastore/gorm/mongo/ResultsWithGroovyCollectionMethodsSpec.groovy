package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import spock.lang.Issue

/**
 * Created by graemerocher on 16/04/14.
 */
class ResultsWithGroovyCollectionMethodsSpec extends GormDatastoreSpec {

    @Issue('GPMONGODB-316')
    void "Test removeAll method on mongo results"() {
        given:"Some test data"
            new Plant(name:"Cabbage", goesInPatch: true).save()
            new Plant(name:"Carrot", goesInPatch: true).save()
            new Plant(name:"Pineapple", goesInPatch: false).save()
            new Plant(name:"Coconut Tree", goesInPatch: false).save()
            new Plant(name:"Lettuce", goesInPatch: true).save(flush:true)
            session.clear()

        when:"A mongo result list is returned and the removeAll method is used on the results"
            def results = Plant.list()

        then:"The origin results are correct"
            results.size() == 5

        when:"The removeAll method is used"
            results.removeAll { !it.goesInPatch }

        then:"It works as expected"
            results.size() == 3
            results.find { it.name == "Cabbage"}
            results.find { it.name == "Carrot"}
            results.find { it.name == "Lettuce"}
    }
}
