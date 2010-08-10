package org.springframework.datastore.redis.collection

import org.springframework.datastore.redis.util.RedisTemplate
import org.jredis.ri.alphazero.JRedisService
import org.jredis.connector.ConnectionSpec
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec
import org.junit.Test
import org.junit.Before

/**
 */
class RedisSetTests {

  RedisTemplate template
  @Before
  void setupRedis() {
    template = createTemplate()
    template.flushdb()
  }

  @Test
  void testSize() {
    
    def set = new RedisSet(template, "test.set")

    assert 0 == set.size()
    assert set.empty
    set.add("test")

    assert 1 == set.size()
    assert !set.empty

  }

  @Test
  void testContains() {
    def set = new RedisSet(template, "test.set")


    assert !set.contains("test")

    set << "test"

    assert set.contains("test")

    set << 1

    assert !set.contains(2)
    assert set.contains(1)

    set.remove(1)
    assert !set.contains(1)
  }

  @Test
  void testIterator() {
    def set = new RedisSet(template, "test.set")

    set << 1 << 2 << 3

    def count = 0
    set.each { count += it.toLong()}

    assert 6 == count
  }

  private RedisTemplate createTemplate() {
    ConnectionSpec connSpec = DefaultConnectionSpec.newSpec();
    connSpec.setSocketProperty(ConnectionSpec.SocketProperty.SO_TIMEOUT, 500);
    def jredisClient = new JRedisService(connSpec,JRedisService.default_connection_count);

    return new RedisTemplate(jredisClient)
  }
}
