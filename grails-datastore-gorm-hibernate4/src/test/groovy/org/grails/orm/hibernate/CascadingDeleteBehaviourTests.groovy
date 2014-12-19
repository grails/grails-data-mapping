package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 0.4
 */
class CascadingDeleteBehaviourTests extends AbstractGrailsHibernateTests {

    @Test
    void testDeleteToOne() {
        def c = new CascadingDeleteBehaviourCompany()
        def p = new CascadingDeleteBehaviourProject()
        def m = new CascadingDeleteBehaviourMember()
        c.save()
        p.company = c
        p.member = m
        p.save()

        session.flush()

        p.delete()
        session.flush()                                     \

        assertEquals 1, CascadingDeleteBehaviourCompany.count()
        assertEquals 0, CascadingDeleteBehaviourMember.count()
        assertEquals 0, CascadingDeleteBehaviourProject.count()
    }

    @Test
    void testDeleteToManyUnidirectional() {
        def c = CascadingDeleteBehaviourCompany.newInstance()

        c.addToLocations(CascadingDeleteBehaviourLocation.newInstance())
        c.addToPeople(CascadingDeleteBehaviourPerson.newInstance())
        c.save()
        session.flush()

        assertEquals 1, CascadingDeleteBehaviourCompany.count()
        assertEquals 1, CascadingDeleteBehaviourLocation.count()
        assertEquals 1, CascadingDeleteBehaviourPerson.count()

        c.delete()
        session.flush()

        assertEquals 0, CascadingDeleteBehaviourCompany.count()
        assertEquals 1, CascadingDeleteBehaviourLocation.count()
        assertEquals 0, CascadingDeleteBehaviourPerson.count()
    }

    @Test
    void testDomainModel() {
        GrailsDomainClass companyClass = ga.getDomainClass(CascadingDeleteBehaviourCompany.name)
        GrailsDomainClass memberClass = ga.getDomainClass(CascadingDeleteBehaviourMember.name)
        GrailsDomainClass projectClass = ga.getDomainClass(CascadingDeleteBehaviourProject.name)
        GrailsDomainClass locationClass = ga.getDomainClass(CascadingDeleteBehaviourLocation.name)
        GrailsDomainClass personClass = ga.getDomainClass(CascadingDeleteBehaviourPerson.name)

        assertFalse companyClass.isOwningClass(memberClass.clazz)
        assertFalse companyClass.isOwningClass(projectClass.clazz)
        assertFalse companyClass.isOwningClass(locationClass.clazz)
        assertFalse companyClass.isOwningClass(personClass.clazz)

        assertFalse projectClass.isOwningClass(companyClass.clazz)
        assertFalse projectClass.isOwningClass(memberClass.clazz)
        assertFalse projectClass.isOwningClass(locationClass.clazz)
        assertFalse projectClass.isOwningClass(personClass.clazz)

        assertTrue memberClass.isOwningClass(projectClass.clazz)
        assertFalse memberClass.isOwningClass(companyClass.clazz)
        assertFalse memberClass.isOwningClass(personClass.clazz)
        assertFalse memberClass.isOwningClass(locationClass.clazz)

        assertTrue personClass.isOwningClass(companyClass.clazz)
        assertFalse personClass.isOwningClass(locationClass.clazz)
        assertFalse personClass.isOwningClass(memberClass.clazz)
        assertFalse personClass.isOwningClass(projectClass.clazz)
    }

    @Override
    protected getDomainClasses() {
        [CascadingDeleteBehaviourPerson,CascadingDeleteBehaviourProject, CascadingDeleteBehaviourCompany, CascadingDeleteBehaviourLocation, CascadingDeleteBehaviourMember]
    }
}

class CascadingDeleteBehaviourCompany {
    Long id
    Long version

    static hasMany = [locations:CascadingDeleteBehaviourLocation, people:CascadingDeleteBehaviourPerson]
    Set locations
    Set people
}
class CascadingDeleteBehaviourPerson {
    Long id
    Long version
    static belongsTo = CascadingDeleteBehaviourCompany
}
class CascadingDeleteBehaviourLocation {
    Long id
    Long version
}
class CascadingDeleteBehaviourProject {
    Long id
    Long version

    CascadingDeleteBehaviourCompany company
    CascadingDeleteBehaviourMember member
}
class CascadingDeleteBehaviourMember {
    Long id
    Long version

    static belongsTo = CascadingDeleteBehaviourProject
}

