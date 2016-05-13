package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class OptLockVersioned implements RxMongoEntity<OptLockVersioned> {
    ObjectId id
    Long version

    String name
}
