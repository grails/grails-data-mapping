package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Created by graemerocher on 20/03/14.
 */
@ApplyDetachedCriteriaTransform
class BatchUpdateDeleteSpec extends GormDatastoreSpec {


    void "Test that batch delete works"() {
        when:"Some test data"
            createTestData()

        then:"The correct amount of data exists"
            Plant.count() == 6

        when:"a batch delete is executed"
            Plant.where {
                name == ~/Ca+/
            }.deleteAll()
            session.flush()

        then:"The right amount of data is deleted"
            Plant.count() == 4
    }

    void "Test that batch update works"() {
        when:"Some test data"
            createTestData()

        then:"The correct amount of data exists"
            Plant.count() == 6

        when:"a batch delete is executed"
            Plant.where {
                name == ~/Ca+/
            }.updateAll(goesInPatch:true)
            session.flush()

        then:"The right amount of data is deleted"
            Plant.countByGoesInPatch(true) == 2
    }

    void createTestData() {
        new Plant(name: "Cabbage").save()
        new Plant(name: "Carrot").save()
        new Plant(name: "Lettuce").save()
        new Plant(name: "Pumpkin").save()
        new Plant(name: "Bamboo").save()
        new Plant(name: "Palm Tree").save(flush:true)
    }
}
