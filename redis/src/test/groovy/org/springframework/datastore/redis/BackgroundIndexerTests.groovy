package org.springframework.datastore.redis

import org.junit.Test
import org.springframework.datastore.redis.util.RedisTemplate
import org.jredis.ri.alphazero.JRedisService
import org.jredis.connector.ConnectionSpec
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec
import org.springframework.datastore.keyvalue.mapping.config.GormKeyValueMappingFactory

/**
 * Tests background indexing functions correctly
 */
class BackgroundIndexerTests {

  @Test
  void testBackgroundIndexer() {


    // create some existing data
    def template = createTemplate()
    template.flushdb()

    template.hmset("org.springframework.datastore.redis.Book:1", [title:"It".bytes])
    template.hmset("org.springframework.datastore.redis.Book:2", [title:"The Shining".bytes])
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
    ConnectionSpec connSpec = DefaultConnectionSpec.newSpec();
    connSpec.setSocketProperty(ConnectionSpec.SocketProperty.SO_TIMEOUT, 500);
    def jredisClient = new JRedisService(connSpec,JRedisService.default_connection_count);

    return new RedisTemplate(jredisClient)
  }

}
