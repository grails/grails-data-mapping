package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Face implements RxMongoEntity<Face> {
    ObjectId id
    Long version
    Nose nose
    String name
    static hasOne = [nose: Nose]
}