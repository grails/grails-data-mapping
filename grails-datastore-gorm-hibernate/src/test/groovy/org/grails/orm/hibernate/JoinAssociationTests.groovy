package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

class JoinAssociationTests extends AbstractGrailsHibernateTests {

    // test for GRAILS-7087
    @Test
    void testObtainCorrectResultsViaJoin() {
        createData()

        def users = JoinAssociationUser.createCriteria().list{
            roles{
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 2, users.head().roles.size()
    }

    // test for GRAILS-7087
    @Test
    void testObtainCorrectResultsViaLeftJoin() {
        createData()

        def users = JoinAssociationUser.createCriteria().list{
            roles(org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN) {
                eq('name', 'Role1')
            }
        }

        assertEquals 1, users.size()
        assertEquals 1, users.head().roles.size()
    }

    private Class createData() {
        def user = JoinAssociationUser.newInstance(name: 'Name')
        user.save(flush: true)

        user.addToRoles(user: user, name: 'Role1')
        user.addToRoles(user: user, name: 'Role2')
        user.save(flush: true)

        session.clear()
        return JoinAssociationUser
    }

    // test for GRAILS-3045
    @Test
    void testObtainCorrectResultWithDistinctPaginationAndJoin() {
        (1..30).each {
            def user = JoinAssociationUser.newInstance(name: "User $it")
            user.save(flush: true)

            if (it % 2) {
                user.addToRoles(user: user, name: 'Role1')
            }
            else {
                user.addToRoles(user: user, name: 'Role2')
            }
        }

        session.flush()
        session.clear()

        def results = JoinAssociationUser.createCriteria().listDistinct {
            roles {
                eq('name', 'Role1')
            }
            order 'id', 'asc'
            maxResults 10
        }

        assert results.size() == 10
    }

    @Override
    protected getDomainClasses() {
        [JoinAssociationUser, JoinAssociationRole]
    }
}
@Entity
class JoinAssociationUser {

    Long id
    Long version

    String name

    static constraints = {
    }

    Set roles
    static hasMany = [roles:JoinAssociationRole]
}

@Entity
class JoinAssociationRole {
    Long id
    Long version

    String name
    JoinAssociationUser user
    static belongsTo = [user:JoinAssociationUser]

    static constraints = {
    }
}
