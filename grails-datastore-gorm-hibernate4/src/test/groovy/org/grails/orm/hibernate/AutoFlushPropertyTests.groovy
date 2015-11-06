package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.datastore.gorm.GormEnhancer
import org.junit.Test
import static junit.framework.Assert.*
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.FlushEvent
import org.hibernate.event.spi.FlushEventListener
import org.hibernate.service.ServiceRegistry

class AutoFlushPropertyTests extends AbstractGrailsHibernateTests {

    private int flushCount = 0
    private band

    private FlushEventListener listener = new FlushEventListener() {
        void onFlush(FlushEvent e) {
            ++flushCount
        }
    }

    protected getDomainClasses() {
        [AutoFlushBand]
    }


    protected void registerHibernateSession() {
        flushCount = 0
        ServiceRegistry serviceRegistry = sessionFactory.serviceRegistry
        EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry)
        listenerRegistry.appendListeners EventType.FLUSH, listener
    }

    protected void onSetUp() {
        band = new AutoFlushBand(name: 'Tool')
        registerHibernateSession()
    }

    @Test
    void testFlushIsDisabledByDefault() {
        assertNotNull band.save()
        band.merge()
        band.delete()
        assertEquals 'Wrong flush count', 0, flushCount
    }

    @Test
    void testFlushPropertyTrue() {
        setAutoFlush true

        assertNotNull band.save()
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge()
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete()
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    @Test
    void testFlushPropertyFalse() {
        setAutoFlush false

        assertNotNull band.save()
        band.merge()
        band.delete()
        assertEquals 'Wrong flush count', 0, flushCount
    }

    @Test
    void testTrueFlushArgumentOverridesFalsePropertySetting() {
        setAutoFlush true

        assert band.save(flush: true)
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge(flush: true)
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete(flush: true)
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    @Test
    void testFalseFlushArgumentOverridesTruePropertySetting() {
        setAutoFlush true

        assertNotNull band.save(flush: false)
        band.merge(flush: false)
        band.delete(flush: false)
        assertEquals 'Wrong flush count', 0, flushCount
    }

    @Test
    void testMapWithoutFlushEntryRespectsTruePropertySetting() {
        setAutoFlush true

        assertNotNull band.save([:])
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge([:])
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete([:])
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    @Test
    void testMapWithoutFlushEntryRespectsFalsePropertySetting() {
        setAutoFlush false

        assertNotNull band.save([:])
        band.merge([:])
        band.delete([:])
        assertEquals 'Wrong flush count', 0, flushCount
    }

    private void setAutoFlush(boolean auto) {
        def config = GormEnhancer.findInstanceApi(AutoFlushBand).config
        if (config == null) {
            config = [:]
            GormEnhancer.findInstanceApi(AutoFlushBand).config = config
        }
        config.autoFlush = auto

        ga.config.grails.gorm.autoFlush = auto
        ga.configChanged()
    }
}

@Entity
class AutoFlushBand {
    Long id
    Long version
    String name
}
