package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class Plant implements Serializable {    
    
    UUID id
    Long version
    boolean goesInPatch
    String name

    static mapping = {
        name index:true
        goesInPatch index:true
    }
}