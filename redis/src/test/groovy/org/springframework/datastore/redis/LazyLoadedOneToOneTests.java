package org.springframework.datastore.redis;

import org.junit.Test;
import org.springframework.datastore.core.Datastore;
import org.springframework.datastore.core.Session;
import static junit.framework.Assert.*;

public class LazyLoadedOneToOneTests {


    @Test
    public void testLazyLoadedOneToOne() {
        Datastore ds = new RedisDatastore();
        ds.getMappingContext().addPersistentEntity(Person.class);
        Session conn = ds.connect(null);

        Person p = new Person();
        p.setName("Bob");
        Address a = new Address();
        a.setNumber("22");
        a.setPostCode("308420");
        p.setAddress(a);
        conn.persist(p);

        p = (Person) conn.retrieve(Person.class, p.getId());

        a = p.getAddress();

        assertTrue(a instanceof org.springframework.aop.SpringProxy);

        assertEquals("22", a.getNumber());
    }
}
