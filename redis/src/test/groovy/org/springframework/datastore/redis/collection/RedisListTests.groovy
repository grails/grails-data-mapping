package org.springframework.datastore.redis.collection

import org.springframework.datastore.redis.util.RedisTemplate
import org.junit.Before
import org.jredis.ri.alphazero.JRedisService
import org.jredis.connector.ConnectionSpec
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec
import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 11, 2010
 * Time: 11:11:50 AM
 * To change this template use File | Settings | File Templates.
 */
class RedisListTests {

  RedisTemplate template
  @Before
  void setupRedis() {
    template = createTemplate()
    template.flushdb()
  }

  @Test
  void testSize() {
    def list = new RedisList(template, "test.list")

    assert 0 == list.size()
    assert list.empty
    list.add("test")

    assert 1 == list.size()
    assert !list.empty
  }

  @Test
  void testIndex() {
    def list = new RedisList(template, "test.list")

    assert 0 == list.size()
    assert list.empty
    list.add("test")

    assert "test" == list[0].toString()

    list[0] = "changed"


    assert list[0] == "changed"

    list << 10

    assert list[1] == 10L
  }

  private RedisTemplate createTemplate() {
    ConnectionSpec connSpec = DefaultConnectionSpec.newSpec();
    connSpec.setSocketProperty(ConnectionSpec.SocketProperty.SO_TIMEOUT, 500);
    def jredisClient = new JRedisService(connSpec,JRedisService.default_connection_count);

    return new RedisTemplate(jredisClient)
  }
}
