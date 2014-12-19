package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.Hibernate

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: May 28, 2008
 */
class EagerFindByQueryTests extends AbstractGrailsHibernateTests {


    @Test
    void testDefaultLazyFetchingGet() {

        assertNotNull EagerFindByQueryBookmark.newInstance(url:"http://grails.org")
                                   .addToTags(name:"groovy")
                                   .addToTags(name:"web")
                                   .addToTags(name:"development")
                                   .save(flush:true)

        session.clear()

        def bookmark = EagerFindByQueryBookmark.get(1)
        assertFalse Hibernate.isInitialized(bookmark.tags)
    }

    @Test
    void testDefaultLazyFetchingFindBy() {

        assertNotNull EagerFindByQueryBookmark.newInstance(url:"http://grails.org")
                                   .addToTags(name:"groovy")
                                   .addToTags(name:"web")
                                   .addToTags(name:"development")
                                   .save(flush:true)

        session.clear()

        def bookmark = EagerFindByQueryBookmark.findByUrl("http://grails.org")

        assertFalse Hibernate.isInitialized(bookmark.tags)
    }

    @Test
    void testEagerFetchingFindBy() {
        assertNotNull EagerFindByQueryBookmark.newInstance(url:"http://grails.org")
                                   .addToTags(name:"groovy")
                                   .addToTags(name:"web")
                                   .addToTags(name:"development")
                                   .save(flush:true)

        session.clear()

        def bookmark = EagerFindByQueryBookmark.findByUrl("http://grails.org",[fetch:[tags:'eager']])
        assertTrue Hibernate.isInitialized(bookmark.tags)
    }

    @Test
    void testEagerFetchingFindAllBy() {
        assertNotNull EagerFindByQueryBookmark.newInstance(url:"http://grails.org")
                                   .addToTags(name:"groovy")
                                   .addToTags(name:"web")
                                   .addToTags(name:"development")
                                   .save(flush:true)

        session.clear()

        def bookmarks = EagerFindByQueryBookmark.findAllByUrl("http://grails.org",[fetch:[tags:'eager']])
        def bookmark = bookmarks[0]

        assertTrue Hibernate.isInitialized(bookmark.tags)
    }

    @Override
    protected getDomainClasses() {
        [EagerFindByQueryTag, EagerFindByQueryBookmark]
    }
}


@Entity
class EagerFindByQueryBookmark {
    Long id
    Long version

    String url
    Set tags
    static hasMany = [tags:EagerFindByQueryTag]
}

@Entity
class EagerFindByQueryTag {
    Long id
    Long version

    String name
}
