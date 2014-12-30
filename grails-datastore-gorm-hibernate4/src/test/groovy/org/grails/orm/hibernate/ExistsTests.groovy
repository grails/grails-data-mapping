package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Burt Beckwith
 */
class ExistsTests extends AbstractGrailsHibernateTests {

    @Test
    void testExistsLongPk() {
        def foo = ExistsFoo.newInstance()
        foo.name = 'foo 1'
        foo.save()

        assertNotNull foo
        assertEquals 1, ExistsFoo.count()
        long fooId = foo.id
        assertTrue ExistsFoo.exists(fooId)
        assertFalse ExistsFoo.exists(fooId + 1)
    }

    @Test
    void testExistsCompositePk() {
        def foo = ExistsFoo.newInstance()
        foo.name = 'foo 1'
        foo.save()
        assertNotNull foo

        def bar = ExistsBar.newInstance()
        bar.name = 'bar 1'
        bar.save()
        assertNotNull bar

        def foobar = ExistsFooBar.newInstance()
        foobar.foo = foo
        foobar.bar = bar
        foobar.d = new Date()

        assertNotNull "should have saved foobar",foobar.save(flush:true)

        session.clear()

        def foobarPk = ExistsFooBar.newInstance()
        foobarPk.foo = foo
        foobarPk.bar = bar

        assertTrue ExistsFooBar.exists(foobarPk)

        def bar2 = ExistsBar.newInstance()
        bar2.name = 'bar 2'
        bar2.save()
        assertNotNull bar2
        def foobarPk2 = ExistsFooBar.newInstance()
        foobarPk.foo = foo
        foobarPk.bar = bar2

        assertFalse ExistsFooBar.exists(foobarPk2)
    }

    @Override
    protected getDomainClasses() {
        [ExistsFoo, ExistsBar, ExistsFooBar]
    }
}
@Entity
class ExistsFoo {
    Long id
    Long version
    String name
}

@Entity
class ExistsBar {
    Long id
    Long version
    String name
}

@Entity
class ExistsFooBar implements Serializable {
    Long id
    Long version
    ExistsFoo foo
    ExistsBar bar
    Date d

    static mapping = {
        id composite: ['foo', 'bar']
    }
}

