package org.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 0.4
 */
class CascadingDeleteBehaviour3Tests extends AbstractGrailsHibernateTests {

    @Test
    void testDeleteToOne() {
        def r = new CascadingDeleteBehaviourRole()
        r.name = "Administrator"
        r.save()

        session.flush()
        def ur = new CascadingDeleteBehaviourUserRole()

        ur.role = r
        ur.username = "Fred"
        ur.save()

        session.flush()
        session.clear()

        ur = CascadingDeleteBehaviourUserRole.get(1)
        ur.delete()
        session.flush()

        r = CascadingDeleteBehaviourRole.get(1)
        assertNotNull r
    }

    @Override
    protected getDomainClasses() {
        [CascadingDeleteBehaviourRole, CascadingDeleteBehaviourUserRole]
    }
}

class CascadingDeleteBehaviourRole {
    Long id
    Long version
    String name
}

class CascadingDeleteBehaviourUserRole {
    Long id
    Long version
    String username
    CascadingDeleteBehaviourRole role
    static belongsTo = [ role: CascadingDeleteBehaviourRole ]
}
