package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class UniqueGroup implements Serializable {
    UUID id
    String name
    static constraints = {
        name unique:true
    }
    
    static mapping = {
        name index:true
    }
}