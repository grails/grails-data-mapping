package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class ModifyPerson implements Serializable {
    String id
    Long version

    String name

    def beforeInsert() {
        name = "Fred"
    }
}