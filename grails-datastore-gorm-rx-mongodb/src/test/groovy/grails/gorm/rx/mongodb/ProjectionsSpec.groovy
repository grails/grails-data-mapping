package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Dog
import spock.lang.Issue

class ProjectionsSpec extends RxGormSpec {

    void "Test sum projection"() {
        given:"Some test data"
        Dog.saveAll(
            new Dog(name:"Fred", age:6),
            new Dog(name:"Ginger", age:2),
            new Dog(name:"Rastas", age:4),
            new Dog(name:"Albert", age:11),
            new Dog(name:"Joe", age:2)
        ).toBlocking().first()

        when:"A sum projection is used"
        def avg = Dog.createCriteria().find {
            projections {
                avg 'age'
                max 'age'
                min 'age'
                sum 'age'
                count()
            }
        }.toBlocking().first()

        then:"The result is correct"
        Dog.count().toBlocking().first() == 5
        avg == [5,11,2,25,5]
    }

    @Issue('GPMONGODB-294')
    void "Test multiple projections"() {
        given:"Some test data"
        new Dog(name:"Fred", age:6).save().toBlocking().first()
        new Dog(name:"Joe", age:2).save(flush:true).toBlocking().first()

        when:"A sum projection is used"
        def results = Dog.createCriteria().list {
            projections {
                property 'name'
                property 'age'
            }
            order 'name'
        }.toBlocking().first()

        then:"The result is correct"
        results == [["Joe", 2], ["Fred", 6]]
    }

    @Override
    List getDomainClasses() {
        [Dog]
    }
}
