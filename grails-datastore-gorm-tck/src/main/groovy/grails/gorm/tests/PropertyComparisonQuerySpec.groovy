package grails.gorm.tests

import grails.persistence.Entity

/**
 * Tests for criteria queries that compare two properties
 */
class PropertyComparisonQuerySpec extends GormDatastoreSpec{

    static {
        TEST_CLASSES << Dog
    }

    void "Test eqProperty query"() {
        given:"Some dead and alive dogs"
            new Dog(name:"Barney", age:7, deathAge:14).save()
            new Dog(name:"Fred", age:13, deathAge:13).save()
            new Dog(name:"Joe", age:4, deathAge:14).save(flush:true)

        when:"We query for dogs that died"
            def results = Dog.withCriteria {
                eqProperty 'age', 'deathAge'
            }

        then:"1 dog is found"
            Dog.count() == 3
            results.size() == 1
            results[0].name == "Fred"
    }
}

@Entity
class Dog implements Serializable{
    Long id
    int age
    int deathAge
    String name

    static mapping = {
        age index:true
        name index:true
    }
}