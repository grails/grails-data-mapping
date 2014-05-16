package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test


/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TransientRangeTests extends AbstractGrailsHibernateTests {

    @Test
    void testTransientRange() {
        def area = AreaType.newInstance(name:"testArea", areaRange:1..10)
        assertNotNull area.save()
    }

    @Override
    protected getDomainClasses() {
        [AreaType]
    }
}

@Entity
class AreaType implements Serializable {

    Long id
    Long version

    static transients = ["areaRange"]

    String name
    Integer rangeFrom
    Integer rangeTo

    static constraints = {
        name(blank:false, unique:true)
    }

    String toString() {
        return name
    }

    Range getAreaRange() {
        return rangeFrom&&rangeTo ? rangeFrom..rangeTo : 0..0
    }

    void setAreaRange(Range range) {
        rangeFrom = range.first()
        rangeTo = range.last()
    }
}
