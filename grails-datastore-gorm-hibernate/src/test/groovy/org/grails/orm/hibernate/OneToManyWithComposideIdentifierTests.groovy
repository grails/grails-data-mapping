package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 20, 2009
 */
class OneToManyWithComposideIdentifierTests extends AbstractGrailsHibernateTests {

    @Test
    void testPersistAssociationWithCompositeId() {
        def content = OneToManyWithComposideIdentifierBlogArticle.newInstance()
        def contentRevision = OneToManyWithComposideIdentifierBlogArticleRevision.newInstance(
                title:"The blog post",
                body:"The body of the post",
                revision: 0)
        content.addToRevisions(contentRevision)
        content.save(flush:true, insert:true)
    }

    @Test
    void testUpdateInverseSide() {
        def content = OneToManyWithComposideIdentifierBlogArticle.newInstance()
        content.save(flush:true)
        def contentRevision = OneToManyWithComposideIdentifierBlogArticleRevision.newInstance(
                title:"The blog post",
                body:"The body of the post",
                revision:0)
        contentRevision.save(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [OneToManyWithComposideIdentifierContent, OneToManyWithComposideIdentifierBlogArticle, OneToManyWithComposideIdentifierBlogArticleRevision, OneToManyWithComposideIdentifierContentRevision]
    }
}


@Entity
class OneToManyWithComposideIdentifierBlogArticle extends OneToManyWithComposideIdentifierContent { }

@Entity
class OneToManyWithComposideIdentifierBlogArticleRevision extends OneToManyWithComposideIdentifierContentRevision {
    String title
    String body
}

@Entity
class OneToManyWithComposideIdentifierContent implements Serializable {
    Long id
    Long version

    Date dateCreated
    Date lastUpdated
    Set revisions
    static hasMany = [ revisions: OneToManyWithComposideIdentifierContentRevision ]
    static mapping = {
        table 'test_content'
    }
}

@Entity
class OneToManyWithComposideIdentifierContentRevision implements Serializable {
    Long id
    Long version

    int revision
    Date dateCreated
    OneToManyWithComposideIdentifierContent content
    static belongsTo = [ content: OneToManyWithComposideIdentifierContent ]
    static mapping = {
        table 'test_content_revision'
        id composite: [ 'content', 'revision' ]
    }
}
