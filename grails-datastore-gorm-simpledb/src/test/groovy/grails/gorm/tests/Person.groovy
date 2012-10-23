package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Person implements Serializable {
    String id
    Long version

    String firstName
    String lastName
    Integer age = 0
    Set pets = [] as Set
    static hasMany = [pets: Pet]

    static peopleWithOlderPets = where {
        pets {
            age > 9
        }
    }
    static peopleWithOlderPets2 = where {
        pets.age > 9
    }
    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", id='" + id + '\'' +
                ", lastName='" + lastName + '\'' +
                ", pets=" + pets +
                '}';
    }
}