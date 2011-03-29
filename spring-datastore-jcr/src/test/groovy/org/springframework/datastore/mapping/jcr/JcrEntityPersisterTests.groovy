package org.springframework.datastore.mapping.jcr

import org.junit.Ignore
import org.junit.Test

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrEntityPersisterTests extends AbstractJcrTest {

    @Ignore
    @Test
    void testConnection() {
        assert null != conn
        assert true == conn.isConnected()

        def session = conn.getNativeInterface()
        assert null != session
    }

    @Test
    void testPersist() {
        ds.mappingContext.addPersistentEntity(TestEntity)
        def t = new TestEntity(title: "foo", body: "bar")
        conn.persist(t)
        assert null != t.id

        t = conn.retrieve(TestEntity, t.id)
        assert t != null
        assert "foo" == t.title
        assert "bar" == t.body
        assert null != t.id

        t.title = 'blog'
        conn.persist(t)

        t = conn.retrieve(TestEntity, t.id)
        assert 'blog' == t.title
        assert 'bar' == t.body

        def id = t.id

        conn.delete(t)
        conn.flush()

        t = conn.retrieve(TestEntity, id)
        assert t == null
    }

    //Still doen't work
    @Ignore
    @Test
    void testTransactions() {
        ds.getMappingContext().addPersistentEntity(TestEntity)
        conn.clear()

        def tx = conn.beginTransaction()

        TestEntity t = new TestEntity(title: "foo", body: "bar")
        conn.persist(t)
        TestEntity t2 = new TestEntity(title: "blog", body: "bar")
        conn.persist(t2)
        tx.commit()

        assert null != t.id
        assert null != t2.id

        tx = conn.beginTransaction()
        TestEntity t3 = new TestEntity(title: "curry", body: "chicken")
        conn.persist(t3)
        TestEntity t4 = new TestEntity(title: "salad", body: "beef")
        conn.persist(t4)
        tx.rollback()

        assert null == t3.id
        assert null == t4.id
    }
}

class TestEntity {
    //using id field as based APIs required,
    //the JCR generated UUID will be assigned to the id property.
    String id

    String title
    String body
}
