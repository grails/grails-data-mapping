package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class CircularManyToManyTests extends AbstractGrailsHibernateTests {


    @Test
    void testCircularManyToManyMapping() {

        def apple = CircularManyToManyOrganization.newInstance(name:"apple")

        assertNotNull "should have saved", apple.save(flush:true)

        def ms = CircularManyToManyOrganization.newInstance(name:"microsoft")
        apple.addToRelatedOrganizations(ms)

        apple.save(flush:true)
        session.clear()

        apple = CircularManyToManyOrganization.get(1)
        assertEquals "apple", apple.name
        assertEquals 1, apple.relatedOrganizations.size()
        assertEquals 0, apple.children.size()

        apple.addToChildren(name:"filemaker")
        apple.save(flush:true)
        session.clear()

        apple = CircularManyToManyOrganization.get(1)
        assertEquals "apple", apple.name
        assertEquals 1, apple.relatedOrganizations.size()
        assertEquals 1, apple.children.size()

        def fm = apple.children.iterator().next()
        assertEquals "filemaker", fm.name
        assertEquals apple, fm.parent
    }

    @Override
    protected getDomainClasses() {
        [CircularManyToManyOrganization]
    }
}


@Entity
class CircularManyToManyOrganization {
    Long id
    Long version

    Set children
    Set relatedOrganizations
    static hasMany = [children: CircularManyToManyOrganization, relatedOrganizations: CircularManyToManyOrganization]
    static mappedBy = [children: "parent", relatedOrganizations:"relatedOrganizations"]

    CircularManyToManyOrganization parent
    String name
}