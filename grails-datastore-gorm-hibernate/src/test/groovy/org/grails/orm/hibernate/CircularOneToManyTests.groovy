package org.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

class CircularOneToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testCircularDomain() {
        def taskDomain = ga.getDomainClass(CircularOneToManyTask.name)
        def tasks = taskDomain?.getPropertyByName("tasks")
        def task = taskDomain?.getPropertyByName("task")
        assertNotNull tasks
        assertNotNull task

        assertTrue tasks.isOneToMany()
        assertTrue tasks.isBidirectional()
        assertTrue task.isManyToOne()
        assertTrue task.isBidirectional()
    }

    @Override
    protected getDomainClasses() {
        [CircularOneToManyTask]
    }
}

class CircularOneToManyTask {
    Long id
    Long version
    Set tasks
    CircularOneToManyTask task
    static belongsTo = CircularOneToManyTask
    static hasMany = [tasks:CircularOneToManyTask]
}

