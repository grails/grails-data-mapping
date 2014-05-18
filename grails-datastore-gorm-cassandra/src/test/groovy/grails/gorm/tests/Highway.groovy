package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@CassandraEntity
class Highway implements Serializable {
    UUID id
    Long version
    Boolean bypassed
    String name
    String other

    static mapping = {
        bypassed index:true
        name index:true
    }
}