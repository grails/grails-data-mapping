package org.springframework.datastore.redis.collection

import org.junit.Test
import org.junit.Before
import org.springframework.datastore.redis.util.JedisTemplate
import redis.clients.jedis.Jedis

/**
 */
class RedisSetTests {

  def template
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

  private def createTemplate() {
    return new JedisTemplate(new Jedis("localhost"))
  }
}
