package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 12/05/16.
 */
@Entity
class Location implements RxMongoEntity<Location> {
    Long version
    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}