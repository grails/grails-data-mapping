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
class UnidirectionalListOneToManyUpdateTests extends AbstractGrailsHibernateTests {


    @Test
    void testAssociateOneToMany() {
        assertNotNull UnidirectionalListCustomer.newInstance(email:"foo@bar.com", password:"letmein").save()
        assertNotNull UnidirectionalListOrder.newInstance(number:"12345").save(flush:true)
        assertNotNull UnidirectionalListOrder.newInstance(number:"12345234").save(flush:true)

        session.clear()

        def cust = UnidirectionalListCustomer.get(1)
        def orders = UnidirectionalListOrder.list()

        orders.each {
            cust.addToOrders(it)
        }

        cust.save(flush:true)

        session.clear()

        cust = UnidirectionalListCustomer.get(1)
        assertEquals 2, cust.orders.size()
    }

    @Override
    protected getDomainClasses() {
        [UnidirectionalListCustomer, UnidirectionalListOrder]
    }
}

@Entity
class UnidirectionalListCustomer {
    Long id
    Long version
    List orders
    static hasMany = [ orders : UnidirectionalListOrder]
    String email
    String password
}

@Entity
class UnidirectionalListOrder {
    Long id
    Long version
    String number
    Date date = new Date()

    static mapping = {
        table "`order`"
    }
}
