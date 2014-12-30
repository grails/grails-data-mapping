package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test


/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jul 21, 2008
 */
class CascadingSaveAndUniqueConstraintTests extends AbstractGrailsHibernateTests {


    @Test
    void testCascadingSaveAndUniqueConstraint() {
        assert new CascadingSaveAndUniqueConstraintFace(nose:new CascadingSaveAndUniqueConstraintNose()).save()
    }

    @Override
    protected getDomainClasses() {
        [CascadingSaveAndUniqueConstraintFace,CascadingSaveAndUniqueConstraintNose]
    }
}


@Entity
class CascadingSaveAndUniqueConstraintFace {
    Long id
    Long version
    CascadingSaveAndUniqueConstraintNose nose

    static constraints = {
        nose(unique: true)
    }
}

@Entity
class CascadingSaveAndUniqueConstraintNose {
    Long id
    Long version
    CascadingSaveAndUniqueConstraintFace face
    static belongsTo = [face:CascadingSaveAndUniqueConstraintFace]
}