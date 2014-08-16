package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Dog implements Serializable{
    UUID id
    int age
    int deathAge
    String name

    static mapping = {
        age index:true
        name index:true
    }
}