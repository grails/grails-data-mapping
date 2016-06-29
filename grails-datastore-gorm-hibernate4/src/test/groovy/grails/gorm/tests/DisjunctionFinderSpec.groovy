package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 06/01/16.
 */
class DisjunctionFinderSpec extends GormSpec {


    Author author1
    Author author2
    Author author3

    Manuscript book1
    Manuscript book2
    Manuscript book3

    def setup() {
        author1 = new Author(name: "Stephen King").save()
        author2 = new Author(name: "Kim Stanley Robinson").save()
        author3 = new Author(name: "Unknown").save()

        book1 = new Manuscript(title: "The Gunslinger").save()
        book2 = new Manuscript(title: "Red Mars").save()
        book3 = new Manuscript(title: "I Ching").save()

        new AuthorManuscript(author: author1, book: book1).save()
        new AuthorManuscript(author: author2, book:  book2).save()
    }

    void "find by author or book -- dynamic finder"() {
        expect:
        AuthorManuscript.findByAuthorOrBook(author3, book3) == null
    }

    void "find by author or book -- criteria"() {
        expect:
        AuthorManuscript.createCriteria().get {
            or {
                eq("author", author3)
                eq("book", book3)
            }
        } == null
    }

    void "find all by author or book -- dynamic finder"() {
        expect:
        AuthorManuscript.findAllByAuthorOrBook(author2, book1).size() == 2
    }

    void "find all by author or book -- criteria"() {
        expect:
        AuthorManuscript.withCriteria {
            or {
                eq("author", author2)
                eq("book", book1)
            }
        }.size() == 2
    }

    void "find by author and book"() {
        expect:
        AuthorManuscript.findByAuthorAndBook(author1, book1) != null
    }

    void "find all by author and book"() {
        expect:
        AuthorManuscript.findAllByAuthorAndBook(author1, book1).size() == 1
    }

    @Override
    List getDomainClasses() {
        [Author, AuthorManuscript, Manuscript]
    }
}
@Entity
class Author {
    String name
}
@Entity
class AuthorManuscript {
    Author author
    Manuscript book
}
@Entity
class Manuscript {
    String title
}
