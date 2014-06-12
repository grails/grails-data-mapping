package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Dog implements Serializable{
    int age
    int deathAge
    String name

    static mapping = {
        age index:true
        name index:true
    }
}