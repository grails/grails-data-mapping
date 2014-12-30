package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jun 2, 2008
 */
class PolymorphicQueryWithAssocationTests  extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [PolymorphicQueryWithAssocationBase, PolymorphicQueryWithAssocationPerson, PolymorphicQueryWithAssocationHyperBase, PolymorphicQueryWithAssocationSpecialBase]
    }

    @Test
    void testQueryPolymorphicAssocation() {
        def p = PolymorphicQueryWithAssocationPerson.newInstance().save()
        assertNotNull PolymorphicQueryWithAssocationHyperBase.newInstance(person: p).save()
        assertNotNull PolymorphicQueryWithAssocationSpecialBase.newInstance(person: p).save()

        assertEquals PolymorphicQueryWithAssocationPerson.findAll().size(), 1
        assertEquals PolymorphicQueryWithAssocationHyperBase.findAll().size(), 1
        assertEquals PolymorphicQueryWithAssocationSpecialBase.findAll().size(), 1
        assertEquals PolymorphicQueryWithAssocationBase.findAll().size(), 2

        assertEquals PolymorphicQueryWithAssocationBase.findAllByPerson(p).size(), 2
    }
}

@Entity
class PolymorphicQueryWithAssocationPerson {
    Long id
    Long version
    Set bases
    static hasMany = [ bases:PolymorphicQueryWithAssocationBase]
}
@Entity
class PolymorphicQueryWithAssocationBase {
    Long id
    Long version

    static belongsTo = PolymorphicQueryWithAssocationPerson
    PolymorphicQueryWithAssocationPerson person
}
@Entity
class PolymorphicQueryWithAssocationHyperBase extends PolymorphicQueryWithAssocationBase {}
@Entity
class PolymorphicQueryWithAssocationSpecialBase extends PolymorphicQueryWithAssocationBase {}
