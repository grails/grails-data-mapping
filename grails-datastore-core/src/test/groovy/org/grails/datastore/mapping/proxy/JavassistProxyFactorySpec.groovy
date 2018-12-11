package org.grails.datastore.mapping.proxy


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
        a.id == 2L
        expect:
        proxyFactory.isProxy(book)
        !proxyFactory.isProxy(a)
        proxyFactory.getIdentifier(book) == 1L
        proxyFactory.getIdentifier(a) == null // not a proxy
        !proxyFactory.isInitialized(book)
        !proxyFactory.isInitialized(a, "book")
    }
}
@grails.gorm.annotation.Entity
class Book {
    Long id
    String title
}
@grails.gorm.annotation.Entity
class Author {
    String name
    Book book
}
