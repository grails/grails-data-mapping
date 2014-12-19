package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * Test for GRAILS-2887.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class CriteriaNegatedAssociationCriterionTests extends AbstractGrailsHibernateTests {


    // test for GRAILS-2887
    @Test
    void testNegatedAssociationCriterion() {
        assertNotNull CNACPerson.newInstance(name:"Bob")
                            .addToRoles(name:"Admin")
                            .save(flush:true)

        assertNotNull CNACPerson.newInstance(name:"Fred")
                            .addToRoles(name:"Admin")
                            .save(flush:true)

        assertNotNull CNACPerson.newInstance(name:"Joe")
                            .addToRoles(name:"Lowlife")
                            .save(flush:true)

        def results = CNACPerson.withCriteria {
            not {
                roles {
                    eq('name', 'Admin')
                }
            }
        }

        assertEquals 1, results.size()
        assertEquals "Joe",  results[0].name

        results = CNACPerson.withCriteria {
            roles {
                eq('name', 'Admin')
            }
        }

        assertEquals 2, results.size()

        results = CNACPerson.withCriteria {
            roles {
                ne('name', 'Admin')
            }
        }

        assertEquals 1, results.size()
        assertEquals "Joe",  results[0].name
    }

    @Override
    protected getDomainClasses() {
        [CNACPerson, CNACRole]
    }
}

@Entity
class CNACPerson {
    Long id
    Long version

    String name
    Set roles
    static hasMany = [roles:CNACRole]
}

@Entity
class CNACRole {
    Long id
    Long version

    String name
}