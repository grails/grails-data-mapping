package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class OptLockNotVersioned implements RxMongoEntity<OptLockNotVersioned> {
    ObjectId id
    Long version

    String name

    static mapping = {
        version false
    }
}