package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId


@Entity
class Carrot implements RxMongoEntity<Carrot> {
    ObjectId id
    Integer leaves
    Animal animal
    static belongsTo = [animal:Animal]
}