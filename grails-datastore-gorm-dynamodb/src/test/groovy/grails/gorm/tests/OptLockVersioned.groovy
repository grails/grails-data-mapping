package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class OptLockVersioned implements Serializable {
    String id
    Long version
    String name
}