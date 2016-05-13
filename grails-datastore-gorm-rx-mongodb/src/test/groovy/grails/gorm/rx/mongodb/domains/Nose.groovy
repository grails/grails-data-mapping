package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Nose implements RxMongoEntity<Node> {
    ObjectId id
    Long version
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]

    static mapping = {
        face index:true
    }
}
