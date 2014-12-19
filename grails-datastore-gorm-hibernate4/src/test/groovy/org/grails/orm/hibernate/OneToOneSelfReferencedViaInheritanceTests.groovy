package org.grails.orm.hibernate

import grails.core.GrailsDomainClass

import static junit.framework.Assert.*
import org.junit.Test


/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 17, 2008
*/
class OneToOneSelfReferencedViaInheritanceTests extends AbstractGrailsHibernateTests{

    @Test
    void testOneToOneSelfReferencingViaInheritance() {
        GrailsDomainClass versionClass = ga.getDomainClass(OneToOneSelfReferencedViaInheritanceVersion.name)
        GrailsDomainClass wikiPageClass = ga.getDomainClass(OneToOneSelfReferencedViaInheritanceWikiPage.name)

        assertTrue wikiPageClass.getPropertyByName("versions").isOneToMany()
        assertTrue wikiPageClass.getPropertyByName("versions").isBidirectional()
        assertTrue versionClass.getPropertyByName("current").isManyToOne()
        assertTrue versionClass.getPropertyByName("current").isBidirectional()
    }

    @Override
    protected getDomainClasses() {
        [OneToOneSelfReferencedViaInheritanceVersion, OneToOneSelfReferencedViaInheritanceContent, OneToOneSelfReferencedViaInheritanceWikiPage]
    }
}

class OneToOneSelfReferencedViaInheritanceContent implements Serializable {
    Long id
    Long version
    String title
    String body

    static mapping = {
        tablePerSubclass true
    }
}

class OneToOneSelfReferencedViaInheritanceVersion extends OneToOneSelfReferencedViaInheritanceContent {
    Integer number
    OneToOneSelfReferencedViaInheritanceContent current
}

class OneToOneSelfReferencedViaInheritanceWikiPage extends OneToOneSelfReferencedViaInheritanceContent {
    Set versions
    static hasMany = [versions:OneToOneSelfReferencedViaInheritanceVersion]
}

