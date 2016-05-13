package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Individual2 implements RxMongoEntity<Individual2> {
    ObjectId id
    String name
    Address address
    List<Address> otherAddresses
    static embedded = ['address', 'otherAddresses']

    static mapping = {
        name index:true
    }
}
