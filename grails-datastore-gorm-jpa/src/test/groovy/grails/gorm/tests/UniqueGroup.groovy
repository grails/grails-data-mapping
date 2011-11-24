package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class UniqueGroup implements Serializable{
    Long id
    String name
    static constraints = {
        name unique:true
    }
    static mapping = {
        table 'groups_table'
        name index:true
    }
}
