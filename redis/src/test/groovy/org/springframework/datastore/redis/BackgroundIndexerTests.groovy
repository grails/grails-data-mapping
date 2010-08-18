package org.springframework.datastore.redis

import org.junit.Test
import org.springframework.datastore.redis.util.RedisTemplate
import sma.RedisClient

/**
 * Tests background indexing functions correctly
 */
class BackgroundIndexerTests {

  @Test
  void testBackgroundIndexer() {


    // create some existing data
    def template = createTemplate()
    template.flushdb()

    template.hmset("org.springframework.datastore.redis.Book:1", [title:"It"])
    template.hmset("org.springframework.datastore.redis.Book:2", [title:"The Shining"])
    template.sadd("org.springframework.datastore.redis.Book.all", 1L)
    template.sadd("org.springframework.datastore.redis.Book.all", 2L)


    // initialise datastore
    def datastore = new RedisDatastore()
    datastore.mappingContext.addPersistentEntity(Book)
    datastore.afterPropertiesSet()

    def session = datastore.connect()

    def results = session.createQuery(Book).eq("title", "It").list()

    assert 1 == results.size()

    
  }

  private RedisTemplate createTemplate() {
    return new RedisTemplate(new RedisClient())
  }

}
