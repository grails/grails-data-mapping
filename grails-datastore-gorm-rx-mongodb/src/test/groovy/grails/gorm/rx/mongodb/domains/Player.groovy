package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import groovy.transform.EqualsAndHashCode

@Entity
@EqualsAndHashCode(includes = 'name')
class Player implements RxMongoEntity<Player> {
    String name
}
