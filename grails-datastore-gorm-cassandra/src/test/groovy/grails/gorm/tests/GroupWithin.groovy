package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class GroupWithin implements Serializable {
    UUID id
    String name
    String org
    
    static constraints = {
        name unique:"org"        
    }
    
    static mapping = {
        name index:true
        org index:true        
    }
}