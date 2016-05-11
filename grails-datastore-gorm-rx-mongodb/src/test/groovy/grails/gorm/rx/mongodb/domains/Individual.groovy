package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Individual implements RxMongoEntity<Individual> {
    String name
    Address address
    static embedded = ['address']

    static mapping = {
        name index:true
    }
}
