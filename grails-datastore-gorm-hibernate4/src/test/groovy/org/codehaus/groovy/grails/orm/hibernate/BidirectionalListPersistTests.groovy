package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 12, 2008
 */
class BidirectionalListPersistTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [TestFaqSection, TestFaqElement]
    }

    @Test
    void testListPersisting() {
        def section = new TestFaqSection()

        section.title = "foo"
        def element = new TestFaqElement()
        element.question = "question 1"
        element.answer = "the answer"
        section.addToElements(element)

        session.save section

        session.flush()
        session.clear()

        section = session.get(TestFaqSection,1L)

        assertNotNull section
        assertEquals 1, section.elements.size()
    }
}

@Entity
class TestFaqSection {
    Long id
    Long version

    String title
    List elements
    static hasMany = [elements:TestFaqElement]
}

@Entity
class TestFaqElement {
    Long id
    Long version

    String question
    String answer
    TestFaqSection section
}