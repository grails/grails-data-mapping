package org.grails.datastore.gorm

import grails.gorm.annotation.Entity
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

/**
 * @author Graeme Rocher
 */
class DirtyCheckingSpec extends GormDatastoreSpec {

    def proxyHandler

    @Override
    List getDomainClasses() {
        [Person, TestBook, TestAuthor]
    }

    def setup() {
        proxyHandler = session.getMappingContext().proxyHandler
    }

    void "Test that dirty checking methods work when changing entities"() {
        when:"A new instance is created"
            def p = new Person(firstName: "Homer", lastName: "Simpson")

        then:"The instance is dirty by default"
            p instanceof DirtyCheckable
            p.isDirty()
            p.isDirty("firstName")

        when:"The instance is saved"
            p.save(flush:true)

        then:"The instance is no longer dirty"
            !p.isDirty()
            !p.isDirty("firstName")

        when:"The instance is changed"
            p.firstName = "Bart"

        then:"The instance is now dirty"
            p.isDirty()
            p.isDirty("firstName")
            p.dirtyPropertyNames == ['firstName']
            p.getPersistentValue('firstName') == "Homer"

        when:"The instance is loaded from the db"
            p.save(flush:true)
            session.clear()
            p = Person.get(p.id)

        then:"The instance is not dirty"
            !p.isDirty()
            !p.isDirty('firstName')

        when:"The instance is changed"
            p.firstName = "Lisa"

        then:"The instance is dirty"
            p.isDirty()
            p.isDirty("firstName")


    }

    void "test relationships not marked dirty when proxies are used"() {

        given:
        Long bookId = new TestBook(title: 'Martin Fierro', author: new TestAuthor(name: 'Jose Hernandez'))
                .save(flush: true)
                .id
        session.flush()
        session.clear()

        when:
        TestBook book = TestBook.get(bookId)
        book.author = book.author

        then:
        proxyHandler.isProxy(book.author)
        !book.isDirty('author')
        !book.isDirty()

        cleanup:
        TestBook.deleteAll()
        TestAuthor.deleteAll()
    }

    void "test relationships not marked dirty when domain objects are used"() {

        given:
        Long bookId = new TestBook(title: 'Martin Fierro', author: new TestAuthor(name: 'Jose Hernandez'))
                .save(flush: true, failOnError: true)
                .id
        session.flush()
        session.clear()

        when:
        TestBook book = TestBook.get(bookId)
        book.author = TestAuthor.get(book.authorId)

        then:
        !proxyHandler.isProxy(book.author)
        !book.isDirty('author')
        !book.isDirty()

        cleanup:
        TestBook.deleteAll()
        TestAuthor.deleteAll()
    }
}

@Entity
class TestAuthor {
    String name
    long version

    @Override
    boolean equals(o) {
        if (!(o instanceof TestAuthor)) return false
        if (this.is(o)) return true
        TestAuthor that = (TestAuthor) o
        if (id !=null && that.id !=null) return id == that.id
        return false
    }
}

@Entity
class TestBook {
    String title
    long version
    TestAuthor author
}
