package org.grails.datastore.mapping.model

import org.junit.Test
import org.grails.datastore.mapping.model.types.*

import static org.junit.Assert.*

/**
 * Tests for correct mapping of entities with inheritance.
 */
class GormMappingInheritanceTests {
    @Test
    void testInheritedMappedBy() {
        def context = new TestMappingContext()
        context.addPersistentEntity(SpecialUser)
        assert 2 == context.persistentEntities.size()

        def user = context.getPersistentEntity(SpecialUser.name)

        Association foesAssociation = user.getPropertyByName("foes")
        assert (foesAssociation instanceof OneToMany)
        assert !foesAssociation.isBidirectional()

        Association friendsAssociation = user.getPropertyByName("friends")
        assert (friendsAssociation instanceof OneToMany)
        assert !friendsAssociation.isBidirectional()

        Association bestBuddyAssociation = user.getPropertyByName("bestBuddy")
        assert (bestBuddyAssociation instanceof OneToOne)
        assert !bestBuddyAssociation.isBidirectional()

        Association specialFriendsAssociation = user.getPropertyByName("specialFriends")
        assert (specialFriendsAssociation instanceof OneToMany)
        assert !specialFriendsAssociation.isBidirectional()

    }

    @Test
    void testInheritedTransients() {
        def context = new TestMappingContext()
        context.addPersistentEntity(DerivedEntity)
        assertEquals 2, context.persistentEntities.size()

        def derived = context.getPersistentEntity(DerivedEntity.name)
        assertNull derived.getPropertyByName('baz')
        assertNull derived.getPropertyByName('bar')
    }

    @Test
    void testInheritedEmbeddeds() {
        def context = new TestMappingContext()
        context.addPersistentEntity(DerivedEmbeddedTest)

        def derived = context.getPersistentEntity(DerivedEmbeddedTest.name)
        def property = derived.getPropertyByName('testEntity')
        assertTrue property instanceof Embedded
        property = derived.getPropertyByName('testEntity2')
        assertTrue property instanceof Embedded
    }

    @Test
    void testInheritedAssociations() {
        def context = new TestMappingContext()
        context.addPersistentEntity(DerivedChild)
        assertEquals 3, context.persistentEntities.size()

        def derivedChild = context.getPersistentEntity(DerivedChild.name)
        Association parentAssociation = derivedChild.getPropertyByName("parent")
        assertTrue parentAssociation instanceof ManyToOne
        assertTrue parentAssociation.bidirectional
        assertEquals parentAssociation.associatedEntity, context.getPersistentEntity(Parent.name)
        assertTrue parentAssociation.inverseSide.owningSide
    }

    @Test
    void testInheritedMapping() {
        def context = new TestMappingContext()
        context.addPersistentEntity(MappingTest2)
        assertEquals 2, context.persistentEntities.size()

        def test = context.getPersistentEntity(MappingTest2.name)
        PersistentProperty property = test.getPropertyByName("toIndex1")
        assertTrue property.mapping.mappedForm.index
        property = test.getPropertyByName("toIndex2")
        assertTrue property.mapping.mappedForm.index
        property = test.getPropertyByName("doNotIndex")
        assertFalse property.mapping.mappedForm.index
    }
}

@grails.persistence.Entity
class DerivedEntity extends SecondEntity {
    String baz

    static transients = ['baz']
}

@grails.persistence.Entity
class SpecialUser extends User {
    Set specialFriends

    static hasMany = [specialFriends: User]

    // prevent bidirectional associations here
    static mappedBy = [specialFriends: null]
}

@grails.persistence.Entity
class Parent {
    Long id
    Set children

    static hasMany = [children: BaseChild]
}

@grails.persistence.Entity
class BaseChild {
    Long id

    Parent parent

    static belongsTo = [parent: Parent]
}

@grails.persistence.Entity
class DerivedChild extends BaseChild {
    String prop
}

@grails.persistence.Entity
class EmbeddedTest {
    Long id

    TestEntity testEntity

    static embedded = ['testEntity']
}

@grails.persistence.Entity
class DerivedEmbeddedTest extends EmbeddedTest {
    TestEntity testEntity2
    static embedded = ['testEntity2']
}

@grails.persistence.Entity
class MappingTest {
    Long id

    String toIndex1

    static mapping = {
        toIndex1 index: true
    }
}

@grails.persistence.Entity
class MappingTest2 extends MappingTest {

    String toIndex2
    String doNotIndex

    static mapping = {
        toIndex2 index: true
        doNotIndex index: false
    }
}

