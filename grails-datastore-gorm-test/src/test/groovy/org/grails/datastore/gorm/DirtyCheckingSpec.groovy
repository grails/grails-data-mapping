package org.grails.datastore.gorm

import grails.gorm.annotation.Entity
import grails.gorm.tests.GormDatastoreSpec

import grails.gorm.tests.Person
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.proxy.EntityProxy

/**
 * @author Graeme Rocher
 */
class DirtyCheckingSpec extends GormDatastoreSpec {

    @java.lang.Override
    List getDomainClasses() {
        return [TestAuthor, TestBook]
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
        Long id
        Long authorId

        TestBook.withNewTransaction {
            TestBook.withSession {
                TestAuthor author = new TestAuthor(name: 'Jose Hernandez')
                TestBook book = new TestBook(title: 'Martin Fierro', author: author)
                book.save(flush: true, failOnError: true)
                id = book.id
                authorId = author.id
            }
        }

        when:
        TestAuthor author
        TestBook book1

        TestBook.withNewTransaction {
            TestBook.withNewSession {
                book1 = TestBook.get(id)
                author = book1.author
                book1.author = author
            }
        }

        then:
        isProxy(author)
        !book1.isDirty('author')
        !book1.isDirty()

        cleanup:
        TestBook.withNewTransaction {
            TestBook.withSession {
                TestAuthor.get(authorId)?.delete()
                TestBook.get(id)?.delete()
            }
        }
    }

    void "test relationships not marked dirty when domain objects are used"() {
        given:
        Long id
        Long authorId

        TestBook.withNewTransaction {
            TestBook.withSession {
                TestAuthor author = new TestAuthor(name: 'Jose Hernandez')
                TestBook book = new TestBook(title: 'Martin Fierro', author: author)
                book.save(flush: true, failOnError: true)
                id = book.id
                authorId = author.id
            }
        }

        when:
        TestAuthor author
        TestBook book1

        TestBook.withNewTransaction {
            TestBook.withNewSession {
                book1 = TestBook.get(id)
                author = TestAuthor.get(authorId)
                book1.author = author

            }
        }

        then:
        !isProxy(author)
        !book1.isDirty('author')
        !book1.isDirty()

        cleanup:
        TestBook.withNewTransaction {
            TestBook.withSession {
                TestAuthor.get(authorId)?.delete()
                TestBook.get(id)?.delete()
            }
        }
    }

    private boolean isProxy(def item) {
        return item instanceof EntityProxy
    }
}

@Entity
class TestAuthor {

    String name
    long version

}

@Entity
class TestBook {

    String title
    long version
    TestAuthor author
}
