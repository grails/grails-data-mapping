package org.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

import org.hibernate.type.TextType

 /**
 * @author Graeme Rocher
 * @since 1.0
 */
class MappingDslTests extends AbstractGrailsHibernateTests {

    @Test
    void testTableMapping() {

        def con
        try {
            con = session.connection()
            con.prepareStatement("select * from people").execute()
        }
        finally {
            con.close()
        }
    }

    @Test
    void testColumnNameMappings() {
        def p = ga.getDomainClass(PersonDSL.name).newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()


        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select First_Name from people")
            def result = statement.executeQuery()
            assertTrue result.next()
            def name = result.getString('First_Name')
            assertEquals "Wilma", name
        }
        finally {
            con.close()
        }
    }

    @Test
    void testDisabledVersion() {
        def p = ga.getDomainClass(PersonDSL.name).newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertNull p.version
    }

    @Test
    void testEnabledVersion() {
        def p = ga.getDomainClass(PersonDSL2.name).newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertEquals 0, p.version

        p.firstName = "Bob"
        p.save()
        session.flush()

        assertEquals 1, p.version
    }

    @Test
    void testCustomHiLoIdGenerator() {
        def p = ga.getDomainClass(PersonDSL.name).newInstance()
        p.firstName = "Wilma"
        p.save()
        session.flush()

        assertNotNull p.id

        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select * from hi_value")
            def result = statement.executeQuery()
            assertTrue result.next()
            def value = result.getLong('next_value')
            assertEquals 1, value
        }
        finally {
            con.close()
        }
    }


    @Test
    void testUserTypes() {
        def relativeClass = ga.getDomainClass(Relative.name)
        def r = relativeClass.newInstance()
        r.firstName = "Wilma"
        r.lastName = 'Flintstone'
        r.save()
        session.flush()

        final cmd = session.getSessionFactory().getClassMetadata(relativeClass.clazz)

        final type = cmd.getPropertyType("firstName")

        assert type instanceof TextType
    }

    @Test
    void testCompositeIdMapping() {
        def compositePersonClass = ga.getDomainClass(CompositePerson.name)
        def cp = compositePersonClass.newInstance()

        cp.firstName = "Fred"
        cp.lastName = "Flintstone"
        cp.save()
        session.flush()
        session.clear()

        cp = compositePersonClass.newInstance()
        cp.firstName = "Fred"
        cp.lastName = "Flintstone"

        def cp1 = compositePersonClass.clazz.get(cp)
        assertNotNull cp1
        assertEquals "Fred", cp1.firstName
        assertEquals "Flintstone", cp1.lastName
    }

    @Test
    void testTablePerSubclassInheritance() {

        def con
        try {
            con = session.connection()
            con.prepareStatement("select * from payment").execute()
            con.prepareStatement("select * from credit_card_payment").execute()
        }
        finally {
            con.close()
        }

        def p = ga.getDomainClass(Payment.name).newInstance()
        p.amount = 10
        p.save()
        session.flush()

        def ccp = ga.getDomainClass(CreditCardPayment.name).newInstance()
        ccp.amount = 20
        ccp.cardNumber = "43438094834380"
        ccp.save()
        session.flush()
        session.clear()

        ccp = ga.getDomainClass(CreditCardPayment.name).clazz.findByAmount(20)
        assertNotNull ccp
        assertEquals 20, ccp.amount
        assertEquals  "43438094834380", ccp.cardNumber
    }

    @Test
    void testOneToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass(MappedPerson.name).clazz
        def addressClass = ga.getDomainClass(MappedAddress.name).clazz

        def p = personClass.newInstance(name:"John")
        p.address = addressClass.newInstance()

        assertNotNull p.save()
        session.flush()

        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select PERSON_ADDRESS_COLUMN from mapped_person")
            def resultSet = statement.executeQuery()
            assert resultSet.next()
        }
        finally {
            con.close()
        }
    }

    @Test
    void testManyToOneForeignKeyMapping() {
        def personClass = ga.getDomainClass(MappedPerson.name).clazz
        def groupClass = ga.getDomainClass(MappedGroup.name).clazz

        def g = groupClass.newInstance()
        g.addToPeople name:"John"
        assertNotNull g.save()

        session.flush()
        session.clear()

        g = groupClass.get(1)
        assertNotNull g
        assertEquals 1, g.people.size()

        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select PERSON_GROUP_COLUMN from mapped_person")
            def resultSet = statement.executeQuery()
            assertTrue resultSet.next()
        }
        finally {
            con.close()
        }
    }

    @Test
    void testManyToManyForeignKeyMapping() {
        def partnerClass = ga.getDomainClass(MappedPartner.name).clazz
        def groupClass = ga.getDomainClass(MappedGroup.name).clazz

        def g = groupClass.newInstance()
        g.addToPartners(partnerClass.newInstance())

        assertNotNull g.save()
        session.flush()
        session.clear()

        g = groupClass.get(1)
        assertNotNull g
        assertEquals 1, g.partners.size()

        def con = session.connection()
        def statement = con.prepareStatement("select PARTNER_JOIN_COLUMN,GROUP_JOIN_COLUMN from PARTNER_GROUP_ASSOCIATIONS")
        def resultSet = statement.executeQuery()
        assertTrue resultSet.next()
    }

    @Test
    void testUnidirectionalOneToManyForeignKeyMapping() {
        def personClass = ga.getDomainClass(MappedPerson.name).clazz
        def childClass = ga.getDomainClass(MappedChild.name).clazz

        def p = personClass.newInstance(name:"John")
        p.addToChildren(childClass.newInstance())
        p.addToCousins(childClass.newInstance())
        p.save()

        assertNotNull p.save()
        session.flush()

        def con
        con = session.connection()
        def statement = con.prepareStatement("select PERSON_ID,COUSIN_ID from COUSINS_TABLE")
        def resultSet = statement.executeQuery()
        assertTrue resultSet.next()
    }

    @Test
    void testCompositeIdAssignedGenerator_GRAILS_6289() {
        def con = session.connection()
        def stmt = con.createStatement()
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_AUTHOR VALUES('Frank Herbert',0)"
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_BOOK VALUES('first','Frank Herbert',0)"
        stmt.executeUpdate "INSERT INTO Composite_Id_Assigned_BOOK VALUES('second','Frank Herbert',0)"

        def authorClass = ga.getDomainClass(CompositeIdAssignedAuthor.name).clazz

        // per GRAILS-6289, this will throw an exception because the afterLoad property cannot be found...
        authorClass.executeQuery 'select distinct a from CompositeIdAssignedAuthor as a inner join fetch a.books'
    }

    @Test
    void testEnumIndex() {
        List<String> indexNames = []
        def connection = session.connection()
        def rs = connection.metaData.getIndexInfo(null, null, 'ENUM_INDEXED', false, false)
        while (rs.next()) { indexNames << rs.getString('INDEX_NAME') }
        rs.close()

        assertTrue indexNames.contains('NAME_INDEX')
        assertTrue indexNames.contains('TRUTH_INDEX')
    }

    @Override
    protected getDomainClasses() {
        [MappedPerson, MappedChild, MappedAddress, MappedGroup, MappedPartner, Payment, CreditCardPayment, CompositePerson, PersonDSL, Relative, EnumIndexed, PersonDSL2, CompositeIdAssignedAuthor, CompositeIdAssignedBook]
    }
}


@Entity
class MappedPerson {
    Long id
    Long version

    String name
    MappedAddress address
    MappedGroup group
    Set children
    Set cousins

    static hasMany = [children:MappedChild, cousins:MappedChild]
    static belongsTo = MappedGroup
    static mapping = {
        columns {
            address column:'PERSON_ADDRESS_COLUMN'
            group column:'PERSON_GROUP_COLUMN'
            children column:'PERSON_CHILD_ID'
            cousins joinTable:[name:'COUSINS_TABLE', key:'PERSON_ID', column:'COUSIN_ID']
        }
    }
    static constraints = {
        group(nullable:true)
        address(nullable:true)
    }
}

@Entity
class MappedChild {

    Long id
    Long version

}

@Entity
class MappedAddress {
    Long id
    Long version

    static belongsTo = MappedPerson
}

@Entity
class MappedGroup {
    Long id
    Long version

    Set people, partners
    static hasMany = [people:MappedPerson, partners:MappedPartner]
    static mapping = {
        columns {
            partners column:'PARTNER_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }
}

@Entity
class MappedPartner {
    Long id
    Long version

    Set groups
    static belongsTo = MappedGroup
    static hasMany = [groups:MappedGroup]
    static mapping = {
        columns {
            groups column:'GROUP_JOIN_COLUMN', joinTable:'PARTNER_GROUP_ASSOCIATIONS'
        }
    }
}

@Entity
class Payment {
    Long id
    Long version

    Integer amount

    static mapping = {
        tablePerHierarchy false
    }
}

@Entity
class CreditCardPayment extends Payment {
    String cardNumber
}

@Entity
class CompositePerson implements Serializable {
    Long id
    Long version

    String firstName
    String lastName

    static mapping = {
        id composite:['firstName', 'lastName']
    }
}

@Entity
class PersonDSL {
    Long id
    Long version

    String firstName

    Set children, cousins
    static hasMany = [children:Relative, cousins:Relative]
    static mapping = {
        table 'people'
        version false
        cache usage:'read-only', include:'non-lazy'
        id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]

        columns {
            firstName name:'First_Name'
            children lazy:false, cache:'read-write'
        }
    }
}

@Entity
class Relative {
    Long id
    Long version


    String firstName
    String lastName

    static mapping = {
        columns {
            firstName type:'text', index:'name_index'
            lastName index:'name_index,other_index'
        }
    }
}

enum Truth {
    TRUE,
    FALSE
}

@Entity
class EnumIndexed {
    Long id
    Long version

    String name
    Truth truth

    static mapping = {
        name index: 'name_index'
        truth index: 'truth_index'
    }
}

@Entity
class PersonDSL2 {
    Long id
    Long version

    String firstName

    static mapping = {
        table 'people2'
        version true
        cache usage:'read-write', include:'non-lazy'
        id column:'person_id'

        columns {
            firstName name:'First_Name'
        }
    }
}
@grails.persistence.Entity
class CompositeIdAssignedAuthor {
    String id
    Long version
    Set books
    static hasMany = [books:CompositeIdAssignedBook]

    static mapping = {
        id column: 'name', generator:'assigned'
    }
}

@grails.persistence.Entity
class CompositeIdAssignedBook implements Serializable {
    Long id
    Long version

    String edition
    CompositeIdAssignedAuthor author

    static mapping = {
        id composite:['edition','author']
    }
}
