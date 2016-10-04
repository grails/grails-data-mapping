package org.grails.datastore.mapping.proxy

import grails.persistence.Entity
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity
import org.grails.datastore.mapping.model.MappingContext
import spock.lang.Specification

/**
 * Created by graemerocher on 04/10/2016.
 */
class JavassistProxyFactorySpec extends Specification {

    void "test is association initialized"() {
        given:
        JavassistProxyFactory proxyFactory = new JavassistProxyFactory()
        def session = Mock(Session)
        def mappingContext = new KeyValueMappingContext("test")
        mappingContext.addPersistentEntities(Book, Author)
        session.getMappingContext() >> mappingContext
        Book book = proxyFactory.createProxy(session, Book, 1L)
        Author a = new Author(book:book)

        expect:
        proxyFactory.isProxy(book)
        !proxyFactory.isInitialized(book)
        !proxyFactory.isInitialized(a, "book")
    }
}
@Entity
class Book {
    Long id
    String title
}
@Entity
class Author {
    String name
    Book book
}
