package grails.gorm.tests

import grails.persistence.Entity

@Entity
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