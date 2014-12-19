package org.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 15, 2007
 */
class EventsBeforeInsertTests extends AbstractGrailsHibernateTests {

    @Test
    void testBeforeInsertEvent() {
        def e = BeforeInsertExample.newInstance()
        e.news = BeforeInsertArticle.newInstance()

        assertNotNull e.save()
        session.flush()

        assertEquals "news", e.moduleName
    }

    @Override
    protected getDomainClasses() {
        [BeforeInsertExample, BeforeInsertArticle]
    }
}
class BeforeInsertExample {

    Long id
    Long version
    BeforeInsertArticle article
    BeforeInsertArticle news
    String moduleName = ''

    static constraints = {
        article(nullable:true)
        news(nullable:true)
    }

    def beforeInsert = {
        if (article) {
            moduleName = 'article'
        }
        else if (news) {
            moduleName = 'news'
        }
    }
}

class BeforeInsertArticle {
    Long id
    Long version
    static belongsTo = BeforeInsertExample
}
