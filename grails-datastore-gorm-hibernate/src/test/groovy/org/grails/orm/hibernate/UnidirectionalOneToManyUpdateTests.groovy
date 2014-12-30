package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Dec 3, 2007
 */
class UnidirectionalOneToManyUpdateTests extends AbstractGrailsHibernateTests {

    @Test
    void testAssociateOneToMany() {
        assertNotNull UnidirectionalOneToManyCustomer.newInstance(email:"foo@bar.com", password:"letmein").save()
        assertNotNull UnidirectionalOneToManyOrder.newInstance(number:"12345").save(flush:true)

        session.clear()

        def cust = UnidirectionalOneToManyCustomer.get(1)
        def order = UnidirectionalOneToManyOrder.get(1)

        cust.addToOrders(order)
        cust.save(flush:true)

        session.clear()

        cust = UnidirectionalOneToManyCustomer.get(1)
        assertEquals 1, cust.orders.size()
    }

    @Override
    protected getDomainClasses() {
        [UnidirectionalOneToManyCustomer, UnidirectionalOneToManyOrder]
    }
}

@Entity
class UnidirectionalOneToManyCustomer {
    Long id
    Long version
    Set orders
    static hasMany = [ orders : UnidirectionalOneToManyOrder]
    String email
    String password
}

@Entity
class UnidirectionalOneToManyOrder {
    Long id
    Long version
    String number
    Date date = new Date()

    static mapping = {
        table "`order`"
    }
}
