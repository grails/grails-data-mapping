package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.hibernate.type.YesNoType

import static junit.framework.Assert.*

import org.junit.Test

/**
* @author Graeme Rocher
*/
class UserTypeMappingTests extends AbstractGrailsHibernateTests{


    @Test
    void testCustomUserType() {
        def person = UserTypeMappingTestsPerson.newInstance(name:"Fred", weight:Weight.newInstance(200))

        person.save(flush:true)
        session.clear()

        person = UserTypeMappingTestsPerson.get(1)

        assertNotNull person
        assertEquals 200, person.weight.pounds
    }

    @Test
    void testUserTypeMapping() {


        assertNotNull UserTypeMappingTest.newInstance(active:true).save(flush:true)


        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select * from type_test")
            def result = statement.executeQuery()
            assertTrue result.next()
            def value = result.getString('active')

            assertEquals "Y", value
        }
        finally {
            con.close()
        }
    }

    @Test
    void testUserTypePropertyMetadata() {
        def personDomainClass = ga.getDomainClass(UserTypeMappingTestsPerson.name)
        def person = UserTypeMappingTestsPerson.newInstance(name:"Fred", weight:Weight.newInstance(200))

        // the metaClass should report the correct type, not Object
        assertEquals Weight, UserTypeMappingTestsPerson.metaClass.hasProperty(person, "weight").type

        // GrailsDomainClassProperty should not appear to be an association
        def prop = personDomainClass.getPropertyByName("weight")
        assertFalse prop.isAssociation()
        assertFalse prop.isOneToOne()
        assertEquals Weight, prop.type
    }

    @Override
    protected getDomainClasses() {
        [UserTypeMappingTest, UserTypeMappingTestsPerson]
    }
}

@Entity
class UserTypeMappingTest {
    Long id
    Long version

    Boolean active

    static mapping = {
        table 'type_test'
        columns {
            active (column: 'active', type: YesNoType)
        }
    }
}


@Entity
class UserTypeMappingTestsPerson {
    Long id
    Long version
    String name
    Weight weight

    static constraints = {
        name(unique: true)
        weight(nullable: true)
    }

    static mapping = {
        columns {
            weight(type:WeightUserType)
        }
    }
}