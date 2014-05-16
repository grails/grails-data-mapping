package grails.gorm.tests

import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException

/**
 * @author Burt Beckwith
 */
class OptimisticLockingSpec extends GormDatastoreSpec {

    void "Test versioning"() {

        given:
            def o = new OptLockVersioned(name: 'locked')

        when:
            o.save flush: true

        then:
            o.version == 0

        when:
            session.clear()
            o = OptLockVersioned.get(o.id)
            o.name = 'Fred'
            o.save flush: true

        then:
            o.version == 1

        when:
            session.clear()
            o = OptLockVersioned.get(o.id)

        then:
            o.name == 'Fred'
            o.version == 1
    }

}
