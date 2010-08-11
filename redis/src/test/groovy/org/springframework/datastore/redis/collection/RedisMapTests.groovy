package org.springframework.datastore.redis.collection

import org.springframework.datastore.redis.util.RedisTemplate
import org.jredis.ri.alphazero.JRedisService
import org.jredis.connector.ConnectionSpec
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec
import org.junit.Before
import org.junit.Test

/**
 */
class RedisMapTests {

  RedisTemplate template
  @Before
  void setupRedis() {
    template = createTemplate()
    template.flushdb()
  }

  @Test
  void testSize() {

    def map = new RedisMap(template, "test.map")

    assert 0 == map.size()
    assert map.empty
    map["foo"] = "bar"

    assert 1 == map.size()
    assert !map.isEmpty()
    assert map["foo"] == "bar"


  }
  private RedisTemplate createTemplate() {
    ConnectionSpec connSpec = DefaultConnectionSpec.newSpec();
    connSpec.setSocketProperty(ConnectionSpec.SocketProperty.SO_TIMEOUT, 500);
    def jredisClient = new JRedisService(connSpec,JRedisService.default_connection_count);

    return new RedisTemplate(jredisClient)
  }

}
