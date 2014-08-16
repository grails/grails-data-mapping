package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Highway implements Serializable {
    UUID id
    Long version
    Boolean bypassed
    String name
    String other

    static mapping = {
        id type:'timeuuid'
        bypassed index:true
        name index:true
    }
}