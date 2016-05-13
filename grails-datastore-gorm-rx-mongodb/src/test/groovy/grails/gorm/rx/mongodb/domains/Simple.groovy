package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 06/05/16.
 */
@Entity
class Simple implements RxMongoEntity<Simple> {
    ObjectId id
    String name
}
