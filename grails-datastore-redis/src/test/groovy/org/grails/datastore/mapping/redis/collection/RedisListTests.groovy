package org.grails.datastore.mapping.redis.collection

import org.junit.Before
import org.junit.Test
import org.grails.datastore.mapping.redis.util.RedisTemplate
import org.grails.datastore.mapping.redis.util.JedisTemplate
import redis.clients.jedis.Jedis

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 11, 2010
 * Time: 11:11:50 AM
 * To change this template use File | Settings | File Templates.
 */
class RedisListTests {

  def template
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

    assert list[1].toLong() == 10
  }

  private RedisTemplate createTemplate() {
    return new JedisTemplate(new Jedis("localhost"))
  }

}
