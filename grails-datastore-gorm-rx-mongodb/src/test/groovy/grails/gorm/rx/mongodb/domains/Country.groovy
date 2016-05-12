package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Country extends Location implements RxMongoEntity<Country> {
    Integer population = 0

    static hasMany = [residents:Person]
    Set residents
}