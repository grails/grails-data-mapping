package org.grails.datastore.mapping.redis.collection

import org.grails.datastore.mapping.redis.util.JedisTemplate
import org.junit.Before
import org.junit.Test

import redis.clients.jedis.Jedis

class RedisMapTests {

    private JedisTemplate template

    @Before
    void setupRedis() {
        template = new JedisTemplate(new Jedis("localhost"))
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
}
