package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

@Issue('https://github.com/grails/grails-data-mapping/issues/617')
class MultiColumnUniqueConstraintSpec extends GormSpec {

    void "test generated unique constraints"() {
        expect:
        new DomainOne(controller: 'project', action: 'update').save(flush:true)
        new DomainOne(controller: 'project', action: 'delete').save(flush:true)
        new DomainOne(controller: 'projectTask', action: 'update').save(flush:true)
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