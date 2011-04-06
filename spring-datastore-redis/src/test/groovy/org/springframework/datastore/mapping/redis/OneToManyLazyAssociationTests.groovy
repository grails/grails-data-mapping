package org.springframework.datastore.mapping.redis

import grails.persistence.Entity

import org.junit.Test
import org.springframework.datastore.mapping.collection.PersistentList
import org.springframework.datastore.mapping.core.Session

class OneToManyLazyAssociationTests extends AbstractRedisTest {

    @Test
    void testOneToManyAssociation() {
        return

        ds.mappingContext.addPersistentEntity(LazyAuthor)
        Session conn = ds.connect()
        conn.nativeInterface.flushall()

        def a = new LazyAuthor(name:"Stephen King")
        a.books = [ new LazyBook(title:"The Stand"), new LazyBook(title:"It")]

        conn.persist(a)
        conn.flush()

        conn.clear()

        a = conn.retrieve(LazyAuthor, a.id)

        assert a != null
        assert "Stephen King" == a.name
        assert a.books != null
        assert a.books instanceof PersistentList
        assert !a.books.isInitialized()
        assert 2 == a.books.size()
        assert a.books.isInitialized()

        def b1 = a.books.find { it.title == 'The Stand'}
        assert b1 != null
        assert b1.id != null
        assert "The Stand" == b1.title
    }
}

@Entity
class LazyAuthor {
    Long id
    String name
    List books
    static hasMany = [books:LazyBook]
}

@Entity
class LazyBook {
    Long id
    String title
}
