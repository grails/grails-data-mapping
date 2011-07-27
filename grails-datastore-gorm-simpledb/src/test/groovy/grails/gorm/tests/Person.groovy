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
    String firstName
    String lastName
    Set pets = [] as Set
    static hasMany = [pets: Pet]

    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", id='" + id + '\'' +
                ", lastName='" + lastName + '\'' +
                ", pets=" + pets +
                '}';
    }

    static mapping = {
        domain 'Person'
    }
}