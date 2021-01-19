package grails.gorm.tests

import grails.gorm.annotation.Entity
import spock.lang.Issue

class RemoveFromSpec extends GormDatastoreSpec {

    @Issue("https://github.com/grails/grails-data-mapping/issues/998")
    void "test removeFrom clears the back reference"() {
        given:
        new AuthorTest(name: "Joe").addToBooks(title: "Ready Player One").addToBooks(title: "Total Recall").save(flush: true, failOnError: true)
        new BookTest(title: "Unrelated").save(flush: true, failOnError: true)
        session.flush()
        session.clear()
        AuthorTest author = AuthorTest.first()
        BookTest book = author.books.find { it.title == "Total Recall" }

        expect:
        author.books.size() == 2

        when:
        author.removeFromBooks(book)
        author.save()
        book.save()
        session.flush()
        session.clear()
        author = AuthorTest.first()

        then:
        author.books.size() == 1
        BookTest.count == 2
    }

    List getDomainClasses() {
        [AuthorTest, BookTest]
    }

}

@Entity
class AuthorTest {
    String name

    static hasMany = [books: BookTest]

    static constraints = {
    }
}

@Entity
class BookTest {
    String title

    static belongsTo = [author: AuthorTest]

    static constraints = {
        author nullable: true
    }
}
