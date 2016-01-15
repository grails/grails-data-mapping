package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 15/01/16.
 */
class DomainGetterSpec extends GormSpec {

    void "test a domain with a getter"() {
        when:
        new DomainOne(controller: 'project', action: 'update').save(flush:true, validate:false)

        then:
        new DomainWithGetter().relatedDomainOne
    }
    @Override
    List getDomainClasses() {
        [DomainOne, DomainWithGetter]
    }
}

@Entity
class DomainWithGetter {
    DomainOne getRelatedDomainOne() {
        DomainOne.findByAction("update")
    }
}
