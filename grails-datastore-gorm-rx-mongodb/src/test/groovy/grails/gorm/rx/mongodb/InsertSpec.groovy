package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Dog
import org.bson.types.ObjectId

class InsertSpec extends RxGormSpec{

    void "test insert method"() {
        when:"An object is inserted"

        def id = new ObjectId()
        Dog d = new Dog(id: id, name: "Fred", age: 10)
        d.insert().toBlocking().first()

        d = Dog.get(id).toBlocking().first()
        then:"The object was persisted with the correct id"
        d.id == id
    }

    @Override
    List<Class> getDomainClasses() {
        [Dog]
    }
}
