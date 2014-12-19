package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import spock.lang.Issue

import static org.junit.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.1
 */
class AssignedGeneratorWithNoVersionTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AssignedGeneratorMember]
    }

    @Test
    @Issue('GRAILS-4049')
    void testPersistentDomain() {

        def mem = new AssignedGeneratorMember(firstName: 'Ilya', lastName: 'Sterin')
        mem.id = 'abc'
        assertNotNull "should have saved entity with assigned identifier", mem.save(flush:true)
    }
}

@Entity
class AssignedGeneratorMember {

    String id
    Long version
    String firstName
    String lastName

    static mapping = {
        table 'members'
        version false
        id column: 'member_name',generator: 'assigned'
    }
}
