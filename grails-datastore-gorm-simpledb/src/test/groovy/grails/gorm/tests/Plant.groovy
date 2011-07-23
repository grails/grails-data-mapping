package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class Plant {
    String id
    boolean goesInPatch
    String name

    static mapping = {
        domain 'Plant'
    }
}