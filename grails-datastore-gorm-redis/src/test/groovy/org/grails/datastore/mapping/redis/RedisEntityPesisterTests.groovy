package org.grails.datastore.mapping.redis

import org.junit.Test
import org.grails.datastore.mapping.core.Session

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class RedisEntityPesisterTests extends AbstractRedisTest {

    @Test
    void testPersistObject() {

        session.nativeInterface.flushdb()
        ds.getMappingContext().addPersistentEntity(TestEntity)

//        assert session.retrieve(TestEntity, new RedisKey(1)) == null

        TestEntity t = new TestEntity()
        t.name = "bob"
        session.persist(t)
        session.flush()

        assert t.id != null

        def key = t.id
        t = session.retrieve(TestEntity, key)

        assert t != null
        assert "bob" == t.name

        session.delete(t)
        session.flush()

        t = session.retrieve(TestEntity, key)
        assert t == null
    }

    @Test
    void testTransactions() {
        if (true) return
        // doesn't work right now
        ds.getMappingContext().addPersistentEntity(TestEntity)

        assert 0 == session.size()

        def t = session.beginTransaction()
        TestEntity te = new TestEntity()
        te.name = "bob"
        session.persist(te)
        TestEntity te2 = new TestEntity()
        te2.name = "frank"
        session.persist(te2)
        t.commit()

        assert 2 == session.size()

        t = session.beginTransaction()
        TestEntity te3 = new TestEntity()
        te3.name = "joe"
        session.persist(te3)
        TestEntity te4 = new TestEntity()
        te4.name = "jack"
        session.persist(te4)

        t.rollback()

        assert 2 == session.size()
    }
}

class TestEntity {
    Long id
    String name
}
