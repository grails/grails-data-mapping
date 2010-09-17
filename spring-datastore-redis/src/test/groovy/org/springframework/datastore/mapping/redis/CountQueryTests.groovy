package org.springframework.datastore.mapping.redis

import org.springframework.datastore.mapping.query.Query

import org.junit.Test
import static org.springframework.datastore.mapping.query.Restrictions.*

/**
 */
class CountQueryTests {

  @Test
  void testDisjunctionAndCount() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Author)
    def session = ds.connect()
    session.getNativeInterface().flushall()

    def a = new Author(name:"Stephen King")
    a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")  ,
            new Book(title:"The Shining")
    ]

    session.persist(a)


    Query q = session.createQuery(Book)
    q
     .disjunction()
     .add( eq( "title", "The Stand" ) )
     .add( eq( "title", "It" ) )
    q.projections()
     .count()

    assert 2 == q.singleResult()


  }

 @Test
  void testSimpleQueryAndCount() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Author)
    def session = ds.connect()
    session.getNativeInterface().flushall()

    def a = new Author(name:"Stephen King")
    a.books = [
            new Book(title:"The Stand"),
            new Book(title:"It")
    ]

    session.persist(a)


    Query q = session.createQuery(Book)

    q.eq("title", "It")
    q.projections().count()

    def result = q.singleResult()

    assert 1 == result

    q = session.createQuery(Book)

    q.eq("title", "The Stand")
    q.projections().count()

    result = q.singleResult()

    assert 1 == result

  }

}
