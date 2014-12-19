package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class AbstractInheritanceTests extends AbstractGrailsHibernateTests {


    @Test
    void testAbstractInheritanceWithOneToMany() {
        def Derived = ga.getDomainClass(AbstractInheritanceDerived.name).clazz
        def Abstract = ga.getDomainClass(AbstractInheritanceAbstractBase.name).clazz
        def One = ga.getDomainClass(AbstractInheritanceOne.name).clazz

        def derived = Derived.newInstance(name:"Bob")

        derived.save(flush:true)

        def one = One.newInstance(derived:derived)

        one.save(flush:true)

        session.clear()

        one = One.get(1)

        assert one.derived != null
        assert one.derived.name == "Bob"
    }

    @Test
    void testGRAILS5356() {
        def Person = ga.getDomainClass(AbstractInheritancePerson.name).clazz
        def Comment = ga.getDomainClass(AbstractInheritanceComment.name).clazz
        def Referral = ga.getDomainClass(AbstractInheritanceReferral.name).clazz
        def Inservice = ga.getDomainClass(AbstractInheritanceInservice.name).clazz
        def Contract = ga.getDomainClass(AbstractInheritanceContract.name).clazz

        def bob = Person.newInstance(name:"Bob")
        bob.save()
        def fred = Person.newInstance(name:"Fred")
        fred.save()

        final now = new Date()
        Referral.newInstance(contractDate:now)
                .addToComments(person:bob)
                .save()

        Inservice.newInstance(contractDate:new Date()-7)
                 .addToComments(person:fred)
                 .save(flush:true)

        session.clear()

        assert Contract.count() == 2
        assert Referral.count() == 1
        assert Inservice.count() == 1

        assert Contract.findByContractDate(now) != null
        assert Contract.findByContractDate(now).typeName == 'Referral'
        assert Contract.createCriteria().get { eq 'contractDate', now} != null

        def referral = Contract.findByContractDate(now)

        assert referral != null
        assert referral.comments != null
        assert referral.comments.size() == 1

        def comment = referral.comments.iterator().next()
        assert comment != null
        assert comment.person.name == 'Bob'
    }

    @Override
    protected getDomainClasses() {
        [AbstractInheritanceContract, AbstractInheritanceOne, AbstractInheritancePerson, AbstractInheritanceDerived, AbstractInheritanceAbstractBase, AbstractInheritanceComment, AbstractInheritanceReferral, AbstractInheritanceInservice, ConcreteGormEnhanced]
    }
}


@Entity
abstract class AbstractInheritanceAbstractBase {
    Long id
    Long version
    Set ones
    static hasMany = [ones:AbstractInheritanceOne]
    String name
}

@Entity
class AbstractInheritanceDerived extends AbstractInheritanceAbstractBase {
}

@Entity
class AbstractInheritanceOne {
    Long id
    Long version

    AbstractInheritanceAbstractBase derived
    static belongsTo = [derived:AbstractInheritanceAbstractBase]
}

@Entity
class AbstractInheritancePerson {
    Long id
    Long version

    String name
}

@Entity
class AbstractInheritanceComment {
    Long id
    Long version

    AbstractInheritancePerson person
    AbstractInheritanceContract contract
    static belongsTo = [person:AbstractInheritancePerson, contract:AbstractInheritanceContract]
}

@Entity
abstract class AbstractInheritanceContract {
    Long id
    Long version

    Set comments
    static hasMany = [comments:AbstractInheritanceComment]

    String name
    Date contractDate

    static transients = [ 'name' ]

    abstract String getTypeName()

    def onLoad() {
        name = getTypeName()
    }
}

@Entity
class AbstractInheritanceReferral extends AbstractInheritanceContract {
    String getTypeName() { return "Referral" }
}

@Entity
class AbstractInheritanceInservice extends AbstractInheritanceContract {
    String getTypeName() { return "Inservice" }
}

abstract class BaseNonGormEnhanced {

    String name
}


@Entity
class ConcreteGormEnhanced extends BaseNonGormEnhanced {
    Long id
    Long version

}