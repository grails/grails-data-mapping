package org.grails.datastore.mapping.redis.collection

import org.junit.Before
import org.junit.Test
import org.grails.datastore.mapping.redis.util.JedisTemplate
import redis.clients.jedis.Jedis

/**
 */
class RedisMapTests {

  def template
  @Before
  void setupRedis() {
    template = createTemplate()
    template.flushdb()
  }

  @Test
  void testSize() {

    def map = new RedisMap(template, "test.map")

    assert 0 == map.size()
    assert map.isEmpty()
    map["foo"] = "bar"

    assert 1 == map.size()
    assert !map.isEmpty()
    assert map["foo"] == "bar"


  }
  private def createTemplate() {
    return new JedisTemplate(new Jedis("localhost"))
  }

}
