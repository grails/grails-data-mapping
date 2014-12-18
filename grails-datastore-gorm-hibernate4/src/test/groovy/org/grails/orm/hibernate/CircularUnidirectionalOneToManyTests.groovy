package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

class CircularUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testCircularDomain() {
        def taskDomain = ga.getDomainClass(CircularUnidirectionalOneToManyTask.name)
        def tasks = taskDomain?.getPropertyByName("tasks")

        assertNotNull tasks
        assertTrue tasks.isOneToMany()
        assertFalse tasks.isBidirectional()
    }

    @Test
    void testOneToMany() {

        def taskParent = CircularUnidirectionalOneToManyTask.newInstance()
        def taskChild = CircularUnidirectionalOneToManyTask.newInstance()

        taskParent.addToTasks(taskChild)
        taskParent.save()
        session.flush()

        session.evict(taskParent)
        session.evict(taskChild)

        taskParent = CircularUnidirectionalOneToManyTask.get(1)

        assertNotNull taskParent
        assertNotNull taskParent.tasks
    }

    @Override
    protected getDomainClasses() {
        [CircularUnidirectionalOneToManyTask]
    }
}
class CircularUnidirectionalOneToManyTask {
    Long id
    Long version
    Set tasks
    static hasMany = [tasks:CircularUnidirectionalOneToManyTask]
}


