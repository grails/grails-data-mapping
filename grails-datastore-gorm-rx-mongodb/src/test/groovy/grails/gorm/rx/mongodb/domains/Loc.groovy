package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.mongodb.geo.Shape
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 11/05/16.
 */
@Entity
class Loc implements RxMongoEntity<Loc> {
    ObjectId id
    String name
    Shape shape

    static mapping = {
        shape geoIndex:'2dsphere'
    }

    @Override
    String toString() {
        name
    }
}