package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

class ManyToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyMappingWithInheritance() {
        def p = Pentagon.newInstance()
        p.addToAttributes(name:"sides", value:"5")

        assertNotNull "should have saved instance", p.save(flush:true)

        session.clear()

        p = Pentagon.get(1)
        assertEquals 1, p.attributes.size()
    }

    @Override
    protected getDomainClasses() {
        [Shape, Pentagon, ShapeAttribute]
    }
}


@Entity
class Pentagon extends Shape {}

@Entity
class Shape {
    Long id
    Long version

    Set attributes
    static hasMany = [attributes:ShapeAttribute]
}

@Entity
class ShapeAttribute {
    Long id
    Long version

    String name
    String value

    Set shapes
    static hasMany = [shapes: Shape]
    static belongsTo = Shape
}
