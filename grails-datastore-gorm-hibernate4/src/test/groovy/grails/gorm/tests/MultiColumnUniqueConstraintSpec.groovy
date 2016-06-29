package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec
import org.hibernate.Session
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Issue

import java.sql.Connection
import java.sql.ResultSet
import java.sql.ResultSetMetaData

@Issue('https://github.com/grails/grails-data-mapping/issues/617')
class MultiColumnUniqueConstraintSpec extends GormSpec {

    void "test generated unique constraints"() {
        expect:
        new DomainOne(controller: 'project', action: 'update').save(flush:true, validate:false)
        new DomainOne(controller: 'project', action: 'delete').save(flush:true, validate:false)
        new DomainOne(controller: 'projectTask', action: 'update').save(flush:true, validate:false)
    }

    void "test generated unique constraints violation"() {
        when:
        new DomainOne(controller: 'project', action: 'update').save(flush:true, validate:false)
        new DomainOne(controller: 'project', action: 'update').save(flush:true, validate:false)

        then:
        thrown DataIntegrityViolationException
    }

    void 'test save 2 distinct objects with independent unique constraints'() {
        when:
        def obj1 = new DomainObject1(objectId: "foo", someUniqueField: "bar").save(flush:true, validate: false)
        def obj2 = new DomainObject2(objectId: "foo", someUniqueField: "bar").save(flush:true, validate: false)

        then:
        obj1 != null
        obj2 != null

        when:
        new DomainObject1(objectId: "foo", someUniqueField: "bar").save(flush:true, validate: false)

        then:
        thrown DataIntegrityViolationException


        when:
        new DomainObject2(objectId: "foo", someUniqueField: "bar").save(flush:true, validate: false)

        then:
        thrown DataIntegrityViolationException
    }

    void "test generated unique constraints for related domains"() {
        given: 'two existing tasks'
            Task task1 = new Task(name: 'task1').save(flush: true, failOnError: true)
            Task task2 = new Task(name: 'task2').save(flush: true, failOnError: true)

        when: 'saving task links for the same toTask but not breaking unique index'
            TaskLink taskLink1 = new TaskLink(fromTask: task1, toTask: task2).save(flush: true, validate: false)
            TaskLink taskLink2 = new TaskLink(fromTask: task2, toTask: task2).save(flush: true, validate: false)

        then: 'both links may be saved'
            taskLink1 
            taskLink2

        when: 'instance which breaks unique index is saved'
            new TaskLink(fromTask: task1, toTask: task2).save(flush: true, validate: false)            

        then: 'DataIntegrityViolationException is thrown'
            thrown DataIntegrityViolationException
    }

    @Override
    List getDomainClasses() {
        [DomainOne, DomainObject1, DomainObject2, Task, TaskLink]
    }
}

@Entity
class DomainOne {

    String controller
    String action

    static constraints = {
        action unique: 'controller'
    }
}


@Entity
class DomainObject1 {
    String objectId
    String someUniqueField

    static constraints = {
        objectId(unique: ['someUniqueField'])
    }
}

@Entity
class DomainObject2 {
    String objectId
    String someUniqueField

    static constraints = {
        objectId(unique: ['someUniqueField'])
    }
}

@Entity
class Task {
    String name
}

@Entity
class TaskLink {

    Task toTask
    Task fromTask

    static constraints = {
        toTask unique: ['fromTask']
    }
}