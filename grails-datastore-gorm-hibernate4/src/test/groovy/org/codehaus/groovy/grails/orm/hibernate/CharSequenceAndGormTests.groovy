package org.codehaus.groovy.grails.orm.hibernate

import grails.gorm.tests.Plant
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CharSequenceAndGormTests extends AbstractGrailsHibernateTests {


    @Test
    void testGormWithStreamCharBuffer() {
        assertNotNull "should have saved instance", new Plant(name:"hello").save(flush:true)
        session.clear()

        def charBuffer = new StreamCharBuffer()
        charBuffer.writer.write('hello')
        assertTrue charBuffer instanceof StreamCharBuffer

        testQueryMethods charBuffer
    }

    @Test
    void testGormWithGString() {

        assertNotNull "should have saved instance", new Plant(name:"hello").save(flush:true)
        session.clear()

        def value = 'hello'
        def queryArg = "${value}"
        assertTrue queryArg instanceof GString
        testQueryMethods queryArg
    }

    private testQueryMethods(queryArg) {
        assert Plant.findByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.countByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.countByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findAllByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findAllByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findWhere(name:queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findAllWhere(name:queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.withCriteria{ eq 'name',queryArg } : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.find("from Plant s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.findAll("from Plant s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert Plant.executeQuery("from Plant s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
    }

    @Override
    protected getDomainClasses() {
        [Plant]
    }
}
