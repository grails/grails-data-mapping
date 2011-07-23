package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class Highway {
    String id
    Boolean bypassed
    String name

    static mapping = {
        domain 'Highway'
    }
}