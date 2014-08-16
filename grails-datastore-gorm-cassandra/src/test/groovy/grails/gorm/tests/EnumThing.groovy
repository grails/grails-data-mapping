package grails.gorm.tests

import grails.persistence.Entity

@Entity
class EnumThing {
    
    UUID id
    Long version
    String name
    TestEnum en

    static mapping = {
        name index: true
        en index: true
    }
}