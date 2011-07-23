package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class Person {
    String id
    String firstName
    String lastName
    Set pets = [] as Set
    static hasMany = [pets:Pet]

    static mapping = {
        domain 'Person'
    }
}