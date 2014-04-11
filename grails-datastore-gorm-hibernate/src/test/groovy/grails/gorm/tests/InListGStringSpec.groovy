package grails.gorm.tests

import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
import spock.lang.Issue

/**
 * Created by graemerocher on 11/04/14.
 */
@ApplyDetachedCriteriaTransform
class InListGStringSpec extends GormDatastoreSpec{

    @Issue('GRAILS-11303')
    void "Test that gstrings can be used with inList queries"() {
        given:"Some test data"
            new Plant(name:"Cabbage").save()
            new Plant(name:"Carrot").save()
            new Plant(name:"Pumpkin").save()
            new Plant(name:"Coconut").save()
            new Plant(name:"Brocoli").save()
            new Plant(name:"Tomato").save(flush:true)

        def plant1 = "Pumpkin"
        def plant2 = "Carrot"

        when:"An InList dynamic finder is used"
            def results = Plant.findAllByNameInList(["$plant1", "$plant2"])

        then:"The results are correct"
            results.size() == 2
            results.find { it.name == "Pumpkin"}
            results.find { it.name == "Carrot"}

        when:"An InList criteria query is used"
            results = Plant.withCriteria() {
                inList 'name', ["$plant1", "$plant2"]
            }

        then:"The results are correct"
            results.size() == 2
            results.find { it.name == "Pumpkin"}
            results.find { it.name == "Carrot"}

        when:"An InList where query is used"
            results = Plant.where() {
                name in ["$plant1", "$plant2"]
            }.list()

        then:"The results are correct"
            results.size() == 2
            results.find { it.name == "Pumpkin"}
            results.find { it.name == "Carrot"}
    }
}
