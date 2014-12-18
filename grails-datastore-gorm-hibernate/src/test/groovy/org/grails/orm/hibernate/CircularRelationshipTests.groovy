package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * Tests a circular relationship.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class CircularRelationshipTests extends AbstractGrailsHibernateTests {

    @Test
    void testCircularRelationship() {
        def child = CircularRelationship.newInstance()
        def parent = CircularRelationship.newInstance()

        child.parent = parent
        parent.addToChildren(child)
        parent.save()

        assertFalse parent.hasErrors()
        session.flush()
        session.clear()

        parent = CircularRelationship.get(1)
        assertEquals 1, parent.children.size()
    }

    @Override
    protected getDomainClasses() {
        [CircularRelationship]
    }
}

@Entity
class CircularRelationship {
    Long id
    Long version

    static hasMany = [children:CircularRelationship]

    CircularRelationship parent
    Set children

    static constraints = {
        parent(nullable:true)
    }
}

