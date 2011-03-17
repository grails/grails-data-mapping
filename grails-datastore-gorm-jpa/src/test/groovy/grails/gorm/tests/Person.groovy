package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Person implements Serializable {
    String firstName
    String lastName
    static hasMany = [pets:Pet]

    static mapping = {
        firstName index:true
        lastName index:true
    }
}