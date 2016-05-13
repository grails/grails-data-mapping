package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import groovy.transform.EqualsAndHashCode
import org.bson.types.ObjectId

@Entity
@EqualsAndHashCode(includes = 'name')
class Player implements RxMongoEntity<Player> {
    ObjectId id
    String name
}
