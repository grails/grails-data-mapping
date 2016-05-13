package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class CityState implements RxMongoEntity<CityState>{
    ObjectId id
    String city
    String state
    int pop
}