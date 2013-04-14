package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Parent implements Serializable {
    String id
    String name
    Set<Child> children = []
    static hasMany = [children: Child]
}