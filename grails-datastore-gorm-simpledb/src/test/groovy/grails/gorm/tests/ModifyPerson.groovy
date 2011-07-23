package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class ModifyPerson {
    String id
    String name

    def beforeInsert() {
        name = "Fred"
    }

    static mapping = {
        domain 'ModifyPerson'
    }
}