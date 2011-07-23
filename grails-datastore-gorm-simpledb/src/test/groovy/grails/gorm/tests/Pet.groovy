package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class Pet {
    String id
    String name
    Date birthDate = new Date()
    PetType type = new PetType(name:"Unknown")
    Person owner

    static mapping = {
        domain 'Pet'
    }
}