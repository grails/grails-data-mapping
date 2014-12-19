package org.grails.orm.hibernate

import grails.gorm.tests.Plant

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class FindByLikeTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [Plant]
    }

    @Test
    void testFindByLikeQuery() {
        new Plant(name: 'ab').save(flush: true)
        new Plant(name: 'aab').save(flush: true)
        new Plant(name: 'abc').save(flush: true)
        new Plant(name: 'abcc').save(flush: true)
        new Plant(name: 'abcc').save(flush: true)
        new Plant(name: 'abcd').save(flush: true)
        new Plant(name: 'abce').save(flush: true)
        new Plant(name: 'abcc').save(flush: true)
        new Plant(name: 'bcc').save(flush: true)
        new Plant(name: 'dbcc').save(flush: true)

        session.clear()

        assert Plant.findAllByNameIlike('%ab%', [max: 5]) : "should have got some results from ilike query"
        assert Plant.findByNameIlike('%ab%') : "should have got a result from ilike query"
    }
}
