package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Person implements RxMongoEntity<Person> {
    String name
}
