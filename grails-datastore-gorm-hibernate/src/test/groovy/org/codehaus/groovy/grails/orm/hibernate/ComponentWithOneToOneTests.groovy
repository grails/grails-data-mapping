package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 19, 2007
 */
class ComponentWithOneToOneTests extends AbstractGrailsHibernateTests {

    @Test
    void testEmbeddedComponentWithOne2One() {
        def u = new Unit(name:"metres",abbreviation:"m")
        def u2 = new Unit(name:"centimetres",abbreviation:"cm")

        u.save()
        u2.save()

        def m1 = new Measurement(value:1.1, unit:u, approximation:true)
        def m2 = new Measurement(value:2.4, unit:u2, approximation:false)

        m1.save()
        m2.save()

        def action = BatchAction.newInstance(sample:m1, sample2:m2, name:"test")

        action.save()
        session.flush()
        session.clear()

        action = BatchAction.get(1)
        assertNotNull action

        assert 1.1 == action.sample.value
        assertEquals "metres", action.sample.unit.name
        assert 2.4 == action.sample2.value
        assertEquals "centimetres", action.sample2.unit.name
    }

    @Override
    protected getDomainClasses() {
        [Unit, Measurement, BatchAction]
    }
}

@Entity
class Unit {
    Long id
    Long version
    String name
    String abbreviation
}

@Entity
class Measurement {
    Long id
    Long version

    Unit unit
    BigDecimal value
    Boolean approximation
}

@Entity
class BatchAction {
    Long id
    Long version

    Measurement sample
    Measurement sample2
    String name
    static embedded = ['sample','sample2']
}