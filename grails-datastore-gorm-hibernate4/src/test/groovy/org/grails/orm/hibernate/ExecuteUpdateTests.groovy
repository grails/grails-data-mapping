package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 28, 2007
 */
class ExecuteUpdateTests extends AbstractGrailsHibernateTests {

    private static final names = ['Fred', 'Bob', 'Ginger']

    private ids = []
    private custClass

    protected getDomainClasses() {
        [Customer]
    }

    def init() {
        custClass = Customer

        for (name in names) {
            def customer = custClass.newInstance(name: name).save()
            assertNotNull customer
            ids << customer.id
        }
    }

    @Test
    void testExecuteUpdate() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer")

        assertEquals 0, custClass.count()
    }

    @Test
    void testExecuteUpdatePositionalParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=?", ['Fred'])

        assertEquals 2, custClass.count()
    }

    @Test
    void testExecuteUpdateOrdinalParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=:name", [name:'Fred'])

        assertEquals 2, custClass.count()
    }

    @Test
    void testExecuteUpdateListParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.id in (:ids)", [ids: ids[0..1]])

        assertEquals 1, custClass.count()
    }

    @Test
    void testExecuteUpdateArrayParams() {
        init()

        assertEquals 3, custClass.count()

        Object[] deleteIds = [ids[0], ids[1]]
        custClass.executeUpdate("delete from Customer c where c.id in (:ids)", [ids: deleteIds])
        assertEquals 1, custClass.count()
    }
}

@Entity
class Customer {
    Long id
    Long version
    String name
}

