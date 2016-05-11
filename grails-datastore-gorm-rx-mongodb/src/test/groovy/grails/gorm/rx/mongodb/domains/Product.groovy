package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Product implements RxMongoEntity<Product> {
    ObjectId id
    String title

    static mapping = {
        index title:"text"
    }

    @Override
    String toString() {
        title
    }
}