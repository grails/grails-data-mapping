package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 11, 2008
 */
class CustomCascadeMappingTests extends AbstractGrailsHibernateTests {


    @Test
    void testCascadingBehaviour() {

        def one = CustomCascadeMappingOne.newInstance()

        shouldFail {
            one.addToFoos(name:"foo1")
               .addToFoos(name:"foo2")
               .save(flush:true)
        }
        one.foos.clear()
        one.addToBars(name:"bar1")
           .addToBars(name:"bar2")
           .save(flush:true)

        session.clear()

        one = CustomCascadeMappingOne.get(1)

        assertEquals 0, one.foos.size()
        assertEquals 2, one.bars.size()
    }

    @Override
    protected getDomainClasses() {
        [CustomCascadeMappingOne, CustomCascadeMappingTwo]
    }
}
@Entity
class CustomCascadeMappingOne {
    Long id
    Long version

    Set foos, bars
    static hasMany = [foos:CustomCascadeMappingTwo, bars:CustomCascadeMappingTwo]

    static mapping = { applicationContext ->
        assert applicationContext != null
        foos cascade:'none', joinTable:'foos'
        bars cascade:'all', joinTable:'bars'
    }
}

@Entity
class CustomCascadeMappingTwo {
    Long id
    Long version

    String name
}