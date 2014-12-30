package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test


class MapMappingJoinTableTests extends AbstractGrailsHibernateTests {

    @Test
    void testTypeMappings() {
        def map = ['freefall':'60 sec','altitude': '14,000 ft']

        def skydive = Skydive.newInstance()

        skydive.jumplog = map
        skydive.save(flush:true)

        session.clear()

        skydive = Skydive.get(1)

        assertEquals 2, skydive.jumplog.size()
        assertEquals '60 sec', skydive.jumplog.freefall

        def c = session.connection()
        def ps = c.prepareStatement("select * from  jump_info")
        def rs = ps.executeQuery()
        assertTrue rs.next()
    }

    @Override
    protected getDomainClasses() {
        [Skydive]
    }
}

@Entity
class Skydive {
    Long id
    Long version

    Map jumplog

    static mapping = {
        jumplog joinTable:[name:"jump_info",column:"map_key"]
    }
}

