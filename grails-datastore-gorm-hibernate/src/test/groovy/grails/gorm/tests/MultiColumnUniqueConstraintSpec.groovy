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
    @Override
    List getDomainClasses() {
        [DomainOne, DomainObject1, DomainObject2]
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