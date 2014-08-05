package org.grails.datastore.mapping.redis

import org.junit.After
import org.junit.Before
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session

abstract class AbstractRedisTest {

    protected RedisDatastore ds
    protected Session session

    @Before
    void setUp() {
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        ds = new RedisDatastore()
        ds.applicationContext = ctx

        session = ds.connect()
        DatastoreUtils.bindSession session
    }

    @After
    void tearDown() {
        session.disconnect()
        ds.destroy()
    }
}
