package org.springframework.datastore.redis;

import org.junit.Test;
import org.springframework.datastore.core.Datastore;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.proxy.EntityProxy;

import static junit.framework.Assert.*;

public class LazyLoadedOneToOneTests {


    @Test
    public void testLazyLoadedOneToOne() {
        Datastore ds = new RedisDatastore();
        ds.getMappingContext().addPersistentEntity(Person.class);
        Session conn = ds.connect();

        Person p = new Person();
        p.setName("Bob");
        Address a = new Address();
        a.setNumber("22");
        a.setPostCode("308420");
        p.setAddress(a);
        conn.persist(p);
        conn.flush();

        conn.clear();

        p = conn.retrieve(Person.class, p.getId());

        Address proxy = p.getAddress();

        assertTrue(proxy instanceof javassist.util.proxy.ProxyObject);
        assertTrue(proxy instanceof EntityProxy);

        EntityProxy ep = (EntityProxy) proxy;
        assertFalse( ep.isInitialized() );
        assertEquals(a.getId(), proxy.getId());

        assertFalse( ep.isInitialized() );
        assertEquals("22", a.getNumber());
    }

    @Test
    public void testProxyMethod() {
        Datastore ds = new RedisDatastore();
        ds.getMappingContext().addPersistentEntity(Person.class);
        Session conn = ds.connect();

        Person p = new Person();
        p.setName("Bob");
        Address a = new Address();
        a.setNumber("22");
        a.setPostCode("308420");
        p.setAddress(a);
        conn.persist(p);

        Person personProxy = (Person) conn.proxy(Person.class, p.getId());

        EntityProxy proxy = (EntityProxy) personProxy;

        assertFalse( proxy.isInitialized() );
        assertEquals( p.getId(), personProxy.getId());

        assertFalse( proxy.isInitialized() );

        assertEquals( "Bob", personProxy.getName());

        assertTrue( proxy.isInitialized() );
    }
}
