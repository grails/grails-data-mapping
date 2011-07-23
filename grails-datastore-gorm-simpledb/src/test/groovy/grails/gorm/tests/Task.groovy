package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

class Task {
    String id
    Set tasks
    Task task
    String name

    static mapping = {
        domain 'Task'
    }

    static hasMany = [tasks:Task]
}