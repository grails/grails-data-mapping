package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Plant implements RxMongoEntity<Plant> {
    ObjectId id
    boolean goesInPatch
    String name

    static mapping = {
        name index:true
        goesInPatch index:true
    }
}