package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Location implements Serializable {
    UUID id
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
