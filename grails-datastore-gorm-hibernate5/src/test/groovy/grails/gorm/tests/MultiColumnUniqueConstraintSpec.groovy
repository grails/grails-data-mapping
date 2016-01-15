package grails.gorm.tests

import grails.persistence.Entity
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Issue

@Issue('https://github.com/grails/grails-data-mapping/issues/617')
class MultiColumnUniqueConstraintSpec extends GormDatastoreSpec {

    void "test generated unique constraints"() {
        expect:
        new DomainOne(controller: 'project', action: 'update').save(flush:true)
        new DomainOne(controller: 'project', action: 'delete').save(flush:true)
        new DomainOne(controller: 'projectTask', action: 'update').save(flush:true)
    }

    void "test generated unique constraints violation"() {
        when:
        new DomainOne(controller: 'project', action: 'update').save(flush:true)
        new DomainOne(controller: 'project', action: 'update').save(flush:true, validate:false)

        then:
        thrown DataIntegrityViolationException
    }
    @Override
    List getDomainClasses() {
        [DomainOne]
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