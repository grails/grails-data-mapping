package org.grails.datastore.mapping.redis

import static org.grails.datastore.mapping.query.Restrictions.*

import org.junit.Test
import org.grails.datastore.mapping.query.Query

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ListQueryTests extends AbstractRedisTest {

    @Test
    void testListQuery() {
        ds.mappingContext.addPersistentEntity(Author)
        session.getNativeInterface().flushall()

        def a = new Author(name:"Stephen King")
        a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")
        ]

        session.persist(a)

        Query q = session.createQuery(Book)

        def results = q.list()

        assert 2 == results.size()

        assert null !=    results.find { it.title == "The Stand" }
        assert null !=    results.find { it.title == "It" }

        q.max 1

        results = q.list()

        assert 1 == results.size()
        assert "The Stand" == results[0].title
    }

    @Test
    void testDisjunction() {
        ds.mappingContext.addPersistentEntity(Author)
        session.getNativeInterface().flushall()

        def a = new Author(name:"Stephen King")
        a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")  ,
            new Book(title:"The Shining")
        ]

        session.persist(a)

        Query q = session.createQuery(Book)
        q.disjunction()
         .add(eq("title", "The Stand"))
         .add(eq("title", "It"))

        def results = q.list()

        assert 2 == results.size()
    }

    @Test
    void testIdProjection() {
        ds.mappingContext.addPersistentEntity(Author)
        session.getNativeInterface().flushall()

        def a = new Author(name:"Stephen King")
        a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")  ,
            new Book(title:"The Shining")
        ]

        session.persist(a)

        Query q = session.createQuery(Book)
        q.disjunction()
         .add(eq("title", "The Stand"))
         .add(eq("title", "It"))
        q.projections().id()

        def results = q.list()

        assert 2 == results.size()
        assert results[0] instanceof Long
    }

    @Test
    void testSimpleQuery() {
        ds.mappingContext.addPersistentEntity(Author)
        session.getNativeInterface().flushall()

        def a = new Author(name:"Stephen King")
        a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")
        ]

        session.persist(a)

        Query q = session.createQuery(Book)

        q.eq("title", "It")

        def results = q.list()

        assert 1 == results.size()
        assert "It" == results[0].title

        q = session.createQuery(Book)

        q.eq("title", "The Stand")

        results = q.list()

        assert 1 == results.size()
        assert "The Stand" == results[0].title
    }
}
