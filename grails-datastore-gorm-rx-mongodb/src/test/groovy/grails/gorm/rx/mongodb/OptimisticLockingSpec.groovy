package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.*
import org.grails.datastore.mapping.core.OptimisticLockingException

class OptimisticLockingSpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        [OptLockVersioned, OptLockNotVersioned]
    }

    void "Test versioning"() {

        given:
        def o = new OptLockVersioned(name: 'locked')

        when:
        o.save().toBlocking().first()

        then:
        o.version == 0

        when:
        o = OptLockVersioned.get(o.id).toBlocking().first()
        o.name = 'Fred'
        o = o.save().toBlocking().first()

        then:
        o.version == 1

        when:
        o = OptLockVersioned.get(o.id).toBlocking().first()

        then:
        o.name == 'Fred'
        o.version == 1
    }

    void "Test optimistic locking"() {

        given:
        def o = new OptLockVersioned(name: 'locked').save(flush: true).toBlocking().first()

        when:
        o = OptLockVersioned.get(o.id).toBlocking().first()

        Thread.start {
                def reloaded = OptLockVersioned.get(o.id).toBlocking().first()
                assert reloaded
                reloaded.name += ' in new session'
                reloaded.save(flush: true).toBlocking().first()
        }.join()
        sleep 2000 // heisenbug

        o.name += ' in main session'
        def ex
        try {
            o.save(flush: true).toBlocking().first()
        }
        catch (e) {
            ex = e
            e.printStackTrace()
        }

        o = OptLockVersioned.get(o.id).toBlocking().first()

        then:
        ex instanceof OptimisticLockingException
        o.version == 1
        o.name == 'locked in new session'
    }

    void "Test optimistic locking disabled with 'version false'"() {

        given:
        def o = new OptLockNotVersioned(name: 'locked').save(flush: true).toBlocking().first()

        when:
        o = OptLockNotVersioned.get(o.id).toBlocking().first()

        Thread.start {
                def reloaded = OptLockNotVersioned.get(o.id).toBlocking().first()
                reloaded.name += ' in new session'
                reloaded.save(flush: true).toBlocking().first()
        }.join()
        sleep 2000 // heisenbug

        o.name += ' in main session'
        def ex
        try {
            o.save(flush: true).toBlocking().first()
        }
        catch (e) {
            ex = e
            e.printStackTrace()
        }

        o = OptLockNotVersioned.get(o.id).toBlocking().first()

        then:
        ex == null
        o.name == 'locked in main session'
    }
}
