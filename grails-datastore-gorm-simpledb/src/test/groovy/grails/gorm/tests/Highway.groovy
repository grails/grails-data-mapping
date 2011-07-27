package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Highway implements Serializable {
    String id
    Boolean bypassed
    String name

    static mapping = {
        domain 'Highway'
    }
}