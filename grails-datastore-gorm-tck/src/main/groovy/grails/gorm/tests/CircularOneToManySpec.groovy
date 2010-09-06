package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 6, 2010
 * Time: 3:40:26 PM
 * To change this template use File | Settings | File Templates.
 */
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

@Entity
class Task {
    Long id
    Set tasks
    Task task
    String name

    static mapping = {
      name index:true
    }
    static hasMany = [tasks:Task]
}