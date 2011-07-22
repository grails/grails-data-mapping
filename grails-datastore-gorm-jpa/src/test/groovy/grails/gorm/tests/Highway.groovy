package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Highway implements Serializable {
    Boolean bypassed
    String name

    static mapping = {
        bypassed index:true
        name index:true
    }
}