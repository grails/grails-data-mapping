package org.codehaus.groovy.grails.orm.hibernate

import grails.gorm.tests.Plant
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 14, 2008
 */
class ExplicitInsertWithSaveMethodTests extends AbstractGrailsHibernateTests {

    @Test
    void testExplicitInsert() {
        def test = new Plant()
        test.name = "Foo"
        assertNotNull test.save(insert:true, flush:true)

        session.clear()

        test = Plant.get(1)
        assertNotNull test
        test.name = "Bar"
        test.save(insert:true, flush:true)
    }

    @Override
    protected getDomainClasses() {
        [Plant]
    }
}

