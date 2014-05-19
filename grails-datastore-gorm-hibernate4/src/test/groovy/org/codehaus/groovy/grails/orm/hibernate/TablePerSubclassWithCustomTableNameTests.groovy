package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test

import javax.sql.DataSource

/**
 * @author grocher
 */
class TablePerSubclassWithCustomTableNameTests extends AbstractGrailsHibernateTests {


    @Override
    protected getDomainClasses() {
        [Animal, Dog, Cat]
    }

    @Test
    void testGeneratedTables() {
        def con
        try {
            con = session.connection()
            def statement = con.prepareStatement("select * from myDogs")
            statement.execute()
            statement = con.prepareStatement("select * from myCats")
            statement.execute()
        } finally {
            con.close()
        }
    }
}

class Animal {
    Long id
    Long version
    String name
    static mapping = {
        tablePerSubclass true
        table "myAnimals"
    }
}

class Dog extends Animal {
    String bark
    static mapping = {
        table "myDogs"
    }
}
class Cat extends Animal {
    String meow
    static mapping = {
        table "myCats"
    }
}