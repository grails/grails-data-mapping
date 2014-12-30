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
class UnidirectionalMapOneToManyUpdateTests extends AbstractGrailsHibernateTests {

    @Test
    void testAssociateOneToMany() {
        assertNotNull UnidirectionalMapCustomer.newInstance(email:"foo@bar.com", password:"letmein").save()
        assertNotNull UnidirectionalMapOrder.newInstance(number:"12345").save(flush:true)

        session.clear()

        def cust = UnidirectionalMapCustomer.get(1)
        def order = UnidirectionalMapOrder.get(1)
        cust.orders = [order1:order]
        cust.save(flush:true)

        session.clear()

        cust = UnidirectionalMapCustomer.get(1)
        assertEquals 1, cust.orders.size()
        assertNotNull cust.orders.order1
    }

    @Override
    protected getDomainClasses() {
        [UnidirectionalMapCustomer, UnidirectionalMapOrder]
    }
}

@Entity
class UnidirectionalMapCustomer {
    Long id
    Long version
    Map orders
    static hasMany = [ orders : UnidirectionalMapOrder]
    String email
    String password
}

@Entity
class UnidirectionalMapOrder {
    Long id
    Long version
    String number
    Date date = new Date()

    static mapping = {
        table "`order`"
    }
}
