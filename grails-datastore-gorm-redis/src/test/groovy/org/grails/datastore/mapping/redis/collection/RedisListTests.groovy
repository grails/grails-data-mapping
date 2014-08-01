package org.grails.datastore.mapping.redis.collection

import org.grails.datastore.mapping.redis.util.JedisTemplate
import org.grails.datastore.mapping.redis.util.RedisTemplate
import org.junit.Before
import org.junit.Test

import redis.clients.jedis.Jedis

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
        assert !list.empty
        assert 1 == list.size()
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
