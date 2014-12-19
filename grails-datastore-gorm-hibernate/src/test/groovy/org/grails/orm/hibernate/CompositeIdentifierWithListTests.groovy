package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class CompositeIdentifierWithListTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeIdentifierWithList() {

        def article = CompositeIdentifierWithListArticle.newInstance()

        article.addToRevisions(title:"one", revision:1)
        assertNotNull "should have saved", article.save(flush:true)

        session.clear()

        article = CompositeIdentifierWithListArticle.get(1)
        assertEquals "one",article.revisions[0].title

        session.clear()

        article = CompositeIdentifierWithListArticle.get(1)
        def revision = CompositeIdentifierWithListArticleRevision.get(CompositeIdentifierWithListArticleRevision.newInstance(article:article, revision:1))
        assertNotNull "many-to-one should have been loaded",revision.article
    }

    @Override
    protected getDomainClasses() {
        [CompositeIdentifierWithListArticle, CompositeIdentifierWithListArticleRevision]
    }
}
@Entity
class CompositeIdentifierWithListArticle {
    Long id
    Long version

    List revisions
    static hasMany = [ revisions: CompositeIdentifierWithListArticleRevision ]

    static constraints = {
        revisions(minSize:1)
    }
}

@Entity
class CompositeIdentifierWithListArticleRevision implements Serializable {
    Long id
    Long version

    String title
    int revision

    CompositeIdentifierWithListArticle article
    static belongsTo = [article:Article]

    static mapping = {
        id composite:['article','revision']
    }
}
