package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Location implements Serializable {
    String id
    Long version

    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }
}