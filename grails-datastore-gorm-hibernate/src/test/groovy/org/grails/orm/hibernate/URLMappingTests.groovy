package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test


class URLMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testURLMapping() {
        def b = new UniListMappingBookmark()

        b.url = new URL("http://grails.org")
        b.publisherSite = new URI("http://apress.com")
        b.title = "TDGTG"
        b.notes = "some notes"
        b.save(flush: true)

        b.discard()
        b = UniListMappingBookmark.get(1)

        assertEquals "http://grails.org", b.url.toString()
    }

    @Override
    protected getDomainClasses() {
        [UniListMappingBookmark]
    }
}

@Entity
class UniListMappingBookmark {
    Long id
    Long version
    URL url
    URI publisherSite
    String title
    String notes
    Date dateCreated = new Date()
}

