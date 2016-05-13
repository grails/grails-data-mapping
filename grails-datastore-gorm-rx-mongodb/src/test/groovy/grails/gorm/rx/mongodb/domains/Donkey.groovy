package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Donkey extends Animal implements RxMongoEntity<Donkey> {
    String name
}
