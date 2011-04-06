package org.springframework.datastore.mapping.redis

import org.junit.Before
import org.springframework.context.support.GenericApplicationContext

abstract class AbstractRedisTest {

    protected RedisDatastore ds

    @Before
    void setUp() {
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        ds = new RedisDatastore()
        ds.applicationContext = ctx
    }
}
