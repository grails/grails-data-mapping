package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test


/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 3, 2008
 */
class NestedProjectionsTests extends AbstractGrailsHibernateTests {


    @Test
    void testNestedProjections() {
        def user = NestedProjectionsUser.newInstance(login:"fred")

        def role = NestedProjectionsRole.newInstance(name:"admin")

        user.addToRoles(role)

        def permission = NestedProjectionsPermission.newInstance(type:"write")

        role.addToPermissions(permission)

        assertNotNull user.save(flush:true)

        assertEquals 1, NestedProjectionsRole.count()
        assertEquals 1, NestedProjectionsPermission.count()

        session.clear()

        def permissions = NestedProjectionsUser.withCriteria {
            projections {
                roles {
                    permissions {
                        property "type"
                    }
                }
            }
            eq("login", "fred")
        }

        assertEquals 1, permissions.size()
        assertEquals "write", permissions.iterator().next()
    }

    @Override
    protected getDomainClasses() {
        [NestedProjectionsUser, NestedProjectionsRole, NestedProjectionsPermission]
    }
}


class NestedProjectionsUser {
    Long id
    Long version

    String login

    Set roles
    static hasMany = [roles:NestedProjectionsRole]
}

class NestedProjectionsRole {
    Long id
    Long version

    String name

    Set permissions
    static hasMany = [permissions:NestedProjectionsPermission]
}

class NestedProjectionsPermission {
    Long id
    Long version

    String type
}
