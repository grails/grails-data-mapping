package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Person implements Serializable {
    String firstName
    String lastName
    Integer age = 0
    static hasMany = [pets:Pet]

    static peopleWithOlderPets = where {
        pets {
            age > 9
        }
    }
    static peopleWithOlderPets2 = where {
        pets.age > 9
    }
    static mapping = {
        firstName index:true
        lastName index:true
    }
}