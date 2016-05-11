package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class Individual2 implements RxMongoEntity<Individual2> {
    String name
    Address address
    List<Address> otherAddresses
    static embedded = ['address', 'otherAddresses']

    static mapping = {
        name index:true
    }
}
