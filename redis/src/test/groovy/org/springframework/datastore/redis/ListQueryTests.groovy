package org.springframework.datastore.redis

import org.jredis.JRedis
import org.junit.Test
import org.springframework.datastore.core.Session
import org.springframework.datastore.query.Query

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ListQueryTests {

  @Test
  void testListQuery() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Author)
    Session<JRedis> session = ds.connect(null)
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

    assert "The Stand" == results[0].title
    assert "It" == results[1].title

    q.max 1

    results = q.list()

    assert 1 == results.size()
    assert "The Stand" == results[0].title
  }

  @Test
  void testSimpleQuery() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Author)
    Session<JRedis> session = ds.connect(null)
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

  }
}

