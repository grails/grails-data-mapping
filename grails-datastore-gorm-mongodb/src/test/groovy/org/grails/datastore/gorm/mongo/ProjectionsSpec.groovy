package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 15/04/14.
 */
class ProjectionsSpec extends GormDatastoreSpec{

    void "Test sum projection"() {
        given:"Some test data"
            new Dog(name:"Fred", age:6).save()
            new Dog(name:"Ginger", age:2).save()
            new Dog(name:"Rastas", age:4).save()
            new Dog(name:"Albert", age:11).save()
            new Dog(name:"Joe", age:2).save(flush:true)

        when:"A sum projection is used"
            def avg = Dog.createCriteria().list {
                projections {
                    avg 'age'
                    max 'age'
                    min 'age'
                    sum 'age'
                    count()
                }
            }

        then:"The result is correct"
            Dog.count() == 5
            avg == [5,11,2,25,5]
    }

    @Override
    List getDomainClasses() {
        [Dog]
    }
}

@Entity
class Dog {
    ObjectId id
    String name
    int age
}
