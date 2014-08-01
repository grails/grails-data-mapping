package org.grails.datastore.mapping.redis

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.redis.util.JedisTemplate
import org.grails.datastore.mapping.redis.util.RedisTemplate

import redis.clients.jedis.Jedis

/**
 * Tests background indexing functions correctly
 */
class BackgroundIndexerTests {

    private RedisDatastore ds
    private Session session

    @Before
    void setUp() {
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        ds = new RedisDatastore()
        ds.applicationContext = ctx
    }

    @After
    void tearDown() {
        session.disconnect()
        ds.destroy()
    }

    @Test
    void testBackgroundIndexer() {

        // create some existing data
        def template = createTemplate()
        template.flushdb()

        template.hmset("org.grails.datastore.mapping.redis.Book:1", [title:"It"])
        template.hmset("org.grails.datastore.mapping.redis.Book:2", [title:"The Shining"])
        template.sadd("org.grails.datastore.mapping.redis.Book.all", 1L)
        template.sadd("org.grails.datastore.mapping.redis.Book.all", 2L)

        // initialise datastore
        ds.mappingContext.addPersistentEntity(Book)
        ds.afterPropertiesSet()

        session = ds.connect()
        DatastoreUtils.bindSession session

        def results = session.createQuery(Book).eq("title", "It").list()

        assert 1 == results.size()
    }

    private RedisTemplate createTemplate() {
        return new JedisTemplate(new Jedis("localhost"))
    }
}
