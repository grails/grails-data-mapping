package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 */
class InheritanceWithAssociationsTests extends AbstractGrailsHibernateTests {

    @Test
    void testMapping() {

        def root = InheritanceWithAssociationsRoot.newInstance()
        def a = InheritanceWithAssociationsA.newInstance()
        def link = InheritanceWithAssociationsLinkToA.newInstance()
        link.a = a
        link.root = root
        a.link = link

        a.save()

        root.addToLinks(link)
        root.save()
        session.flush()
        session.clear()

        root = InheritanceWithAssociationsRoot.get(1)
        assertNotNull root
        assertEquals 1, root.links.size()

        link = root.links.iterator().next()
        assertNotNull link
        assertNotNull link.a
    }

    @Override
    protected getDomainClasses() {
        [InheritanceWithAssociationsA, InheritanceWithAssociationsB, InheritanceWithAssociationsLink, InheritanceWithAssociationsLinkToA, InheritanceWithAssociationsLinkToB, InheritanceWithAssociationsRoot]
    }
}

class InheritanceWithAssociationsA {
    Long id
    Long version
    InheritanceWithAssociationsLinkToA link
}

class InheritanceWithAssociationsB {
    Long id
    Long version
    InheritanceWithAssociationsLinkToB link
}

class InheritanceWithAssociationsLink {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsRoot

    InheritanceWithAssociationsRoot root

    static constraints = {
        root(nullable:true)
    }
}

class InheritanceWithAssociationsLinkToA extends InheritanceWithAssociationsLink {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsA
    InheritanceWithAssociationsA a
}

class InheritanceWithAssociationsLinkToB {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsB
    InheritanceWithAssociationsB b
}

class InheritanceWithAssociationsRoot {
    Long id
    Long version

    Set links
    static hasMany = [links : InheritanceWithAssociationsLink]
}
