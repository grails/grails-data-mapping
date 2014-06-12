package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Task implements Serializable {
    String id
    Long version

    Set tasks
    Task task
    String name

    static mapping = {
        domain 'Task'
    }

    static hasMany = [tasks:Task]
}