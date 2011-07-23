package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class OptLockVersioned {
    String id
    String name

    static mapping = {
        domain 'OptLockVersioned'
    }
}