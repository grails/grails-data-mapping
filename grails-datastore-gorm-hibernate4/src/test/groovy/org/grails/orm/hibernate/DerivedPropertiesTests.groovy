package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

class DerivedPropertiesTests extends AbstractGrailsHibernateTests {

    @Test
    void testDerivedPropertyDefinedInSubclassIsNotConstrained() {
        def mdc = ga.getDomainClass(SubClassDerivedProperties.name)
        def mc = mdc.clazz
        assertFalse 'three should not have been constrained', mdc.constrainedProperties.containsKey('three')
    }

    @Test
    void testDerivedPropertiesCannotBeMadeValidateable() {

        def myDomainClass = ga.getDomainClass(ClassWithConstrainedDerivedProperty.name)
        def myClass = myDomainClass.clazz

        assertFalse 'numberTwo should not have been constrained', myDomainClass.constrainedProperties.containsKey('numberTwo')

        def obj = myClass.newInstance()
        obj.numberOne = 500

        assertTrue 'validation should have passed', obj.validate()
        assertNotNull obj.save(flush: true)

        session.clear()

        obj = myClass.findByNumberOne(500)
        assertEquals 1500, obj.numberTwo
        assertTrue 'validation should have passed', obj.validate()
    }

    @Test
    void testDerivedPropertyValues() {
        [10, 20, 30].each { price ->
            def product = DerivedPropertiesProduct.newInstance()
            product.price = price
            assertNotNull 'saving product failed', product.save(flush: true)
        }
        session.clear()

        assertEquals 30, DerivedPropertiesProduct.findByPrice(10)?.finalPrice
        assertEquals 60, DerivedPropertiesProduct.findByPrice(20)?.finalPrice
        assertEquals 90, DerivedPropertiesProduct.findByPrice(30)?.finalPrice
    }

    @Test
    void testQueryWithDynamicFinders() {

        [10, 20, 30].each { price ->
            def product = DerivedPropertiesProduct.newInstance()
            product.price = price
            assertNotNull 'saving product failed', product.save(flush: true)
        }
        session.clear()

        def cnt = DerivedPropertiesProduct.count()
        assertEquals 3, cnt

        cnt = DerivedPropertiesProduct.countByFinalPrice(60)
        assertEquals 1, cnt

        cnt = DerivedPropertiesProduct.countByFinalPriceGreaterThan(50)
        assertEquals 2, cnt
    }

    @Test
    void testQueryWithCriteria() {

        [10, 20, 30].each { price ->
        def product = DerivedPropertiesProduct.newInstance()
            product.price = price
            assertNotNull 'saving product failed', product.save(flush: true)
        }
        session.clear()

        def cnt = DerivedPropertiesProduct.createCriteria().count {
            eq 'finalPrice', 60
        }
        assertEquals 1, cnt

        cnt = DerivedPropertiesProduct.createCriteria().count {
            gt 'finalPrice', 50
        }
        assertEquals 2, cnt
    }

    @Test
    void testDerivedPropertiesAreNotConstrained() {
        def productDomainClass = ga.getDomainClass(DerivedPropertiesProduct.name)

        assertFalse 'finalPrice should not have been constrained', productDomainClass.constrainedProperties.containsKey('finalPrice')
    }

    @Test
    void testNullabilityOfDerivedPropertiesSurvivesRefreshConstraints() {
        def productDomainClass = ga.getDomainClass(DerivedPropertiesProduct.name)
        assertFalse 'finalPrice should not have been constrained', productDomainClass.constrainedProperties.containsKey('finalPrice')
        def productClass = productDomainClass.clazz

        def product = productClass.newInstance()
        product.price = 40

        assertTrue 'validation should have passed before refreshing constraints', product.validate()

        productDomainClass.refreshConstraints()
        assertFalse 'finalPrice should not have been constrained', productDomainClass.constrainedProperties.containsKey('finalPrice')
        assertTrue 'validation should have passed after refreshing constraints', product.validate()
    }

    @Override
    protected getDomainClasses() {
        [DerivedPropertiesProduct, ClassWithConstrainedDerivedProperty, DerivedPropertiesAbstractBaseClass, SubClassDerivedProperties]
    }
}

@Entity
class DerivedPropertiesProduct {
    Long id
    Long version

    Integer price
    Integer finalPrice
    static mapping = {
        finalPrice formula: 'PRICE * 3'
    }
}

@Entity
class ClassWithConstrainedDerivedProperty {
    Long id
    Long version

    Integer numberOne
    Integer numberTwo
    static mapping = {
        numberTwo formula: 'NUMBER_ONE * 3'
    }
    static constraints = {
        // should be ignored...
        numberTwo max: 10
    }
}

@Entity
class DerivedPropertiesAbstractBaseClass {
    Long id
    Long version

    String one
    String two
}

@Entity
class SubClassDerivedProperties extends DerivedPropertiesAbstractBaseClass {
    Long id
    Long version

    String three
    static mapping = {
        three formula: 'CONCAT(one, two)'
    }
}
