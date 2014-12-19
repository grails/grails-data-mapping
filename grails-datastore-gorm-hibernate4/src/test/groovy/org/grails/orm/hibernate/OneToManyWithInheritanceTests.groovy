package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Sep 21, 2007
 */
class OneToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

    @Test
    void testPersistentAndLoad() {
        def owner = OneToManyWithInheritanceOwnerObject.newInstance()
        owner.name = "The Owner"

        def s1 = OneToManyWithInheritanceSubClass1.newInstance()
        s1.name = "An Object"
        s1.owner = owner

        def s2 = OneToManyWithInheritanceSubClass2.newInstance()
        s2.otherField = "The Field"
        s2.owner = owner

        owner.addToClass1(s1)
        owner.addToClass2(s2)
        owner.save()

        session.flush()
        session.clear()

        owner = OneToManyWithInheritanceOwnerObject.get(1)

        s1 = owner.class1.iterator().next()
        s2 = owner.class2.iterator().next()

        assertNotNull s1
        assertNotNull s2

        assertEquals "An Object", s1.name
        assertEquals "The Field", s2.otherField
    }

    @Override
    protected getDomainClasses() {
        [OneToManyWithInheritanceOwnerObject, OneToManyWithInheritanceBaseClass, OneToManyWithInheritanceSubClass1, OneToManyWithInheritanceSubClass2]
    }
}

class OneToManyWithInheritanceOwnerObject {
    Long id
    Long version
    String name

    Set class1
    Set class2
    static hasMany = [class1: OneToManyWithInheritanceSubClass1, class2: OneToManyWithInheritanceSubClass2]
}

class OneToManyWithInheritanceSubClass1 extends OneToManyWithInheritanceBaseClass {
    Long id
    Long version
    String name

    String toString() {
        return "SubClass1 - $name"
    }
}

class OneToManyWithInheritanceSubClass2 extends OneToManyWithInheritanceBaseClass {
    Long id
    Long version
    String otherField

    String toString() {
        return "SubClass2 - $otherField"
    }
}

class OneToManyWithInheritanceBaseClass {
    Long id
    Long version
    OneToManyWithInheritanceOwnerObject owner
    Date created = new Date()

    static belongsTo = OneToManyWithInheritanceOwnerObject
}