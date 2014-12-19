package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.FlushMode

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 *
 * Created: Sep 19, 2008
 */
class DirtyCheckWithValidationTests extends AbstractGrailsHibernateTests {

    @Test
    void testDataBindingAndValidationWithDirtyChecking() {
        session.setFlushMode FlushMode.AUTO


        DirtyCheckWithValidation.newInstance(name:"valid").save(flush:true)

        def test = DirtyCheckWithValidation.get(1)
        test.name = ''
        assertFalse test.validate()
        session.flush()
        session.clear()

        test = DirtyCheckWithValidation.get(1)
        assertEquals 'valid', test.name
    }

    @Test
    void testRetrySaveWithDataBinding() {
        session.setFlushMode FlushMode.AUTO

        DirtyCheckWithValidation.newInstance(name:"valid").save(flush:true)

        def test = DirtyCheckWithValidation.get(1)
        test.name = ''
        assertFalse test.validate()
        session.flush()
        session.clear()
        test = DirtyCheckWithValidation.get(1)

        assertEquals 'valid', test.name

        test.name = ''
        assertNull test.save(flush:true)

        test.name = 'thisisgood'
        assertNotNull test.save(flush:true)

        session.clear()

        test = DirtyCheckWithValidation.get(1)
        assertEquals 'thisisgood', test.name
    }

    @Override
    protected getDomainClasses() {
        [DirtyCheckWithValidation]
    }
}

@Entity
class DirtyCheckWithValidation {
    Long id
    Long version

    String name

    static constraints = {
        name blank:false
    }
}
