package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CompositeIdentifierNoVersionTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeIdentifierWithNoVersion() {

        def content = BlogArticle.newInstance()
        def revision = BlogArticleRevision.newInstance(title:"Test",body:"The Body", revision:0)
        content.addToRevisions(revision)

        assertNotNull content.save(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [CompositeIdentifierNoVersionContent, CompositeIdentifierNoVersionContentRevision, BlogArticle, BlogArticleRevision]
    }
}
@Entity
class BlogArticle extends CompositeIdentifierNoVersionContent { }

@Entity
class BlogArticleRevision extends CompositeIdentifierNoVersionContentRevision {
    String title
    String body
}

@Entity
class CompositeIdentifierNoVersionContent implements Serializable {
    Long id
    Long version

    Date dateCreated
    Date lastUpdated
    Set revisions
    static hasMany = [ revisions: CompositeIdentifierNoVersionContentRevision ]
    static mapping = {
        table 'content'
        version false
    }
    static constraints = {
        revisions(minSize:1)
    }
}

@Entity
class CompositeIdentifierNoVersionContentRevision implements Serializable {
    Long id
    Long version

    int revision
    Date dateCreated
    CompositeIdentifierNoVersionContent content
    static belongsTo = [ content: CompositeIdentifierNoVersionContent ]
    static mapping = {
        table 'content_revision'
        version false
        id composite: [ 'content', 'revision' ]
    }
}

