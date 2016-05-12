package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.RxEntity
import org.bson.types.ObjectId

@Entity
class Dog implements RxEntity<Dog>{
    ObjectId id
    String name
    int age
}