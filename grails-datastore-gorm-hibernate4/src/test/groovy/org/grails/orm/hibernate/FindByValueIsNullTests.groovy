package org.grails.orm.hibernate

import grails.persistence.Entity
import groovy.transform.NotYetImplemented
import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jun 22, 2009
 */
class FindByValueIsNullTests extends AbstractGrailsHibernateTests {

    @Test
    @NotYetImplemented
    void testFindByIsNull() {

        // test for GRAILS-4601
        assertNotNull "should have saved domain",FindByValueIsNull.newInstance(name:"Bob", age:11).save()
        assertNotNull "should have saved domain",FindByValueIsNull.newInstance(name:"Fred").save()

        session.flush()
        session.clear()

        def results = FindByValueIsNull.findAllByAgeIsNull()
        assertNotNull "should have returned results", results
        assertEquals 1, results.size()
        assertEquals "Fred", results[0].name

        session.clear()

        results = FindByValueIsNull.findAllByBeeIsNull()
        assertNotNull "should have returned results", results
        assertEquals 2, results.size()
    }

    @Override
    protected getDomainClasses() {
        [FindByValueIsNull, FindByValueIsNullB]
    }
}


@Entity
class FindByValueIsNull {
    Long id
    Long version

    String name
    Integer age
    FindByValueIsNullB bee
    static belongsTo = [bee:FindByValueIsNullB]
    static constraints = {
        age nullable:true
        bee nullable:true
    }
}
@Entity
class FindByValueIsNullB {
    Long id
    Long version

    String name
    FindByValueIsNull aye

    static constraints = {
        aye nullable:true
    }
}
