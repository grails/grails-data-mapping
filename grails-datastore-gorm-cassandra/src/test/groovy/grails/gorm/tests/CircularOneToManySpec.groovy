package grails.gorm.tests

import spock.lang.Ignore


/**
 * @author graemerocher
 */
@Ignore("Cassandra GORM does not support associations at present")
class CircularOneToManySpec extends GormDatastoreSpec{

    void "Test circular one-to-many"() {
        given:
            def parent = new Task(name:"Root").save()
            def child = new Task(task:parent, name:"Finish Job").save(flush:true)
            session.clear()

        when:
            parent = Task.findByName("Root")
            child = Task.findByName("Finish Job")

        then:
            parent.task == null
            child.task.id == parent.id
    }
}