package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class GroupWithin implements Serializable{
    Long id
    String name
    String org
    static constraints = {
        name unique:"org"
    }
}