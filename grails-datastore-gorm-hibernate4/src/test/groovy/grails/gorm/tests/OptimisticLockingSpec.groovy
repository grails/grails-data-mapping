package grails.gorm.tests

import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException

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

    void "Test optimistic locking"() {

        given:
            def o = new OptLockVersioned(name: 'locked').save(flush: true)
            session.clear()
            setupClass.transactionManager.commit setupClass.transactionStatus
            setupClass.transactionStatus = null

        when:
            o = OptLockVersioned.get(o.id)

            OptLockVersioned.withNewSession { s ->
                def reloaded = OptLockVersioned.get(o.id)
                assert reloaded
                reloaded.name += ' in new session'
                reloaded.save(flush: true)
            }

            o.name += ' in main session'
            def ex
            try {
                o.save(flush: true)
            }
            catch (e) {
                ex = e
                e.printStackTrace()
            }

            session.clear()
            o = OptLockVersioned.get(o.id)

        then:
            ex instanceof HibernateOptimisticLockingFailureException
            o.version == 1
            o.name == 'locked in new session'
    }

    void "Test optimistic locking disabled with 'version false'"() {
        given:
            def o = new OptLockNotVersioned(name: 'locked').save(flush: true)
            session.clear()
            setupClass.transactionManager.commit setupClass.transactionStatus
            setupClass.transactionStatus = null

        when:
            o = OptLockNotVersioned.get(o.id)

            OptLockNotVersioned.withNewSession { s ->
                def reloaded = OptLockNotVersioned.get(o.id)
                reloaded.name += ' in new session'
                reloaded.save(flush: true)
            }

            o.name += ' in main session'
            def ex
            try {
                o.save(flush: true)
            }
            catch (e) {
                ex = e
                e.printStackTrace()
            }

            session.clear()
            o = OptLockNotVersioned.get(o.id)

        then:
            ex == null
            o.name == 'locked in main session'
    }
}
