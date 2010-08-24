package org.springframework.datastore.redis.collection

import org.springframework.datastore.redis.util.RedisClientTemplate
import org.junit.Before
import org.junit.Test
import sma.RedisClient

/**
 */
class RedisMapTests {

  RedisClientTemplate template
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
  private RedisClientTemplate createTemplate() {
    return new RedisClientTemplate(new RedisClient())
  }

}
