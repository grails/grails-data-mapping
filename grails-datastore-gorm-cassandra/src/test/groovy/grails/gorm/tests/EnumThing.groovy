package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
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