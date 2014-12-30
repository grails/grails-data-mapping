package org.grails.orm.hibernate

import grails.persistence.Entity;

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

@Entity
class InheritanceWithAssociationsA {
    Long id
    Long version
    InheritanceWithAssociationsLinkToA link
}

@Entity
class InheritanceWithAssociationsB {
    Long id
    Long version
    InheritanceWithAssociationsLinkToB link
}

@Entity
class InheritanceWithAssociationsLink {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsRoot

    InheritanceWithAssociationsRoot root

    static constraints = {
        root(nullable:true)
    }
}

@Entity
class InheritanceWithAssociationsLinkToA extends InheritanceWithAssociationsLink {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsA
    InheritanceWithAssociationsA a
}

@Entity
class InheritanceWithAssociationsLinkToB {
    Long id
    Long version

    static belongsTo = InheritanceWithAssociationsB
    InheritanceWithAssociationsB b
}

@Entity
class InheritanceWithAssociationsRoot {
    Long id
    Long version

    Set links
    static hasMany = [links : InheritanceWithAssociationsLink]
}
