package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class EnumThing implements Serializable {
    String id
    Long version

    String name
    TestEnum en
}