package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class PetType {
    String id
    String name

    static belongsTo = Pet

    static mapping = {
        domain 'PetType'
    }
}