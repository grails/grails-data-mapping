package org.grails.datastore.mapping.jcr

import static org.grails.datastore.mapping.query.Restrictions.*

import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.grails.datastore.mapping.query.Query

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class ListQueryTests extends AbstractJcrTest {

    @After
    void clearNodes() {
        def session = conn.getNativeInterface()
        def wp = session.getWorkspace()
        def qm = wp.getQueryManager()

        def q = qm.createQuery("//Book", javax.jcr.query.Query.XPATH)
        def qr = q.execute()
        def itr = qr.getNodes()
        itr.each { it.remove() }

        q = qm.createQuery("//Author", javax.jcr.query.Query.XPATH)
        qr = q.execute()
        itr = qr.getNodes()
        itr.each { it.remove() }
        session.save()
    }

    @Test
    void testListQuery() {
        ds.mappingContext.addPersistentEntity(Author)

        def a = new Author(name: "Stephen King")
        a.books = [new Book(title: "The Stand"),
                   new Book(title: "It")]

        conn.persist(a)

        Query q = conn.createQuery(Book)

        def results = q.list()

        assert 2 == results.size()

        //assert "The Stand" == results[0].title
        //assert "It" == results[1].title

        assert null !=    results.find { it.title == "The Stand" }
        assert null !=    results.find { it.title == "It" }

        q.max 1

        results = q.list()

        assert 1 == results.size()
        //assert "The Stand" == results[0].title
    }

    @Test
    void testDisjunction() {
        ds.mappingContext.addPersistentEntity(Author)

        def a = new Author(name: "Stephen King")
        a.books = [new Book(title: "The Stand"),
                   new Book(title: "It"),
                   new Book(title: "The Shining")]

        conn.persist(a)


        Query q = conn.createQuery(Book)

        q.disjunction().add(eq("title", "The Stand"))
                       .add(eq("title", "The Shining"))

        def results = q.list()

        assert 2 == results.size()
        assert null != results.find { it.title == "The Stand" }
        assert null != results.find { it.title == "The Shining" }
        assert null == results.find { it.title == "It" }
    }

    @Test
    void testIdProjection() {
        ds.mappingContext.addPersistentEntity(Author)

        def a = new Author(name: "Stephen King")
        a.books = [new Book(title: "The Stand"),
                   new Book(title: "It"),
                   new Book(title: "The Shining")]

        conn.persist(a)


        Query q = conn.createQuery(Book)
        q.disjunction().add(eq("title", "The Stand"))
                       .add(eq("title", "It"))
        q.projections().id()


        def results = q.list()

        assert 2 == results.size()
        assert results[0] instanceof String
    }

    @Test
    void testSimpleQuery() {
        ds.mappingContext.addPersistentEntity(Author)

        def a = new Author(name: "Stephen King")
        a.books = [new Book(title: "The Stand"),
                   new Book(title: "It")]

        conn.persist(a)

        Query q = conn.createQuery(Book)

        q.eq("title", "It")

        def results = q.list()

        assert 1 == results.size()
        assert "It" == results[0].title

        q = conn.createQuery(Book)

        q.eq("title", "The Stand")

        results = q.list()

        assert 1 == results.size()
        assert "The Stand" == results[0].title
    }
}
