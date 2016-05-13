package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Animal implements RxMongoEntity<Animal> {
    String id
    Set carrots = []
    static hasMany = [carrots:Carrot]
}