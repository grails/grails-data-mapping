package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CascadeValidationForDomainSubClassTests extends AbstractGrailsHibernateTests {


    @Test
    void testCascadingValidation() {

        def b = new CascadeValidationForDomainSubClassBook(title:'War & Peace', pages:9999) // pages violates range constraint
        def a = new CascadeValidationForDomainSubClassNovelist(name:'Tolstoy', book:b)
        assertFalse "Should have failed validation for subclass!", a.validate()
    }

    @Override
    protected getDomainClasses() {
        [CascadeValidationForDomainSubClassAuthor, CascadeValidationForDomainSubClassBook, CascadeValidationForDomainSubClassNovelist]
    }
}

@Entity
class CascadeValidationForDomainSubClassBook {
    Long id
    Long version

    String title
    Integer pages
    static belongsTo = [CascadeValidationForDomainSubClassAuthor]
    static constraints = { pages(range: 0..100) }
}

@Entity
class CascadeValidationForDomainSubClassAuthor {
    Long id
    Long version

    String name
    CascadeValidationForDomainSubClassBook book
}

@Entity
class CascadeValidationForDomainSubClassNovelist extends CascadeValidationForDomainSubClassAuthor {}