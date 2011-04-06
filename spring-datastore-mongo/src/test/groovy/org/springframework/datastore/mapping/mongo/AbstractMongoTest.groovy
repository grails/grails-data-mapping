package org.springframework.datastore.mapping.mongo

import org.junit.Before
import org.springframework.context.support.GenericApplicationContext

abstract class AbstractMongoTest {

    protected MongoDatastore md

    @Before
    void setUp() {
        md = new MongoDatastore()
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        md.applicationContext = ctx
        md.afterPropertiesSet()
    }
}
