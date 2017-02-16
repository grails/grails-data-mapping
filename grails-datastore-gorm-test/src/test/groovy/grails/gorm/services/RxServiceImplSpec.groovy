package grails.gorm.services

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.gorm.rx.services.implementers.CountByObservableImplementer
import org.grails.gorm.rx.services.implementers.CountObservableImplementer
import org.grails.gorm.rx.services.implementers.CountWhereObservableImplementer
import org.grails.gorm.rx.services.implementers.DeleteObservableImplementer
import org.grails.gorm.rx.services.implementers.FindAndDeleteObservableImplementer
import org.grails.gorm.rx.services.implementers.FindOneByObservableImplementer
import org.grails.gorm.rx.services.implementers.FindOneObservableImplementer
import org.grails.gorm.rx.services.implementers.FindOneObservableStringQueryImplementer
import org.grails.gorm.rx.services.implementers.FindOneObservableWhereImplementer
import org.grails.gorm.rx.services.implementers.SaveObservableImplementer
import org.grails.gorm.rx.services.implementers.UpdateObservableStringQueryImplementer
import org.grails.gorm.rx.services.implementers.UpdateOneObservableImplementer
import rx.Single
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Created by graemerocher on 15/02/2017.
 */
class RxServiceImplSpec extends Specification {
    @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
        Book
    )


    void "test find method that returns an observable"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        expect:"the observable returns the correct res"
        bookService.countByTitleLike("The%").toBlocking().value() == 1
        bookService.countFor("The Stand").toBlocking().value() == 1
        bookService.count("The Stand").toBlocking().first() == 1
        bookService.find("The Stand").toList().toBlocking().single().size() == 1
        bookService.findOne("The Stand").toBlocking().value().title == "The Stand"
        bookService.findByTitleLike("The%").toBlocking().first().title == "The Stand"
    }

    void "test delete method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        def result = bookService.delete("The Stand").toBlocking().value()

        then:
        result == 1

        when:
        bookService.find("The Stand").toBlocking().first() == null

        then:
        thrown(NoSuchElementException)

    }

    void "test find and delete method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        Book result = bookService.deleteOne("The Stand").toBlocking().value()

        then:
        result != null
        result.title == "The Stand"

        when:
        bookService.find("The Stand").toBlocking().first() == null

        then:
        thrown(NoSuchElementException)

    }

    void "test find with string query method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        def result = bookService.findWithQuery("The Stand").toBlocking().first()

        then:
        thrown(UnsupportedOperationException)

        when:
        bookService.updateBook("The Stand", "It").toBlocking().first()

        then:
        thrown(UnsupportedOperationException)

    }

    void "test find with where query method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        Book result = bookService.findWhereTitle("The Shining").toBlocking().first()

        then:
        result != null
        result.title == "The Shining"

    }

    void "test save method"() {
        given:
        BookService bookService = datastore.getService(BookService)

        when:
        Book savedBook = bookService.saveBook("The Shining").toBlocking().value()
        Book result = bookService.findWhereTitle("The Shining").toBlocking().first()


        then:
        savedBook != null
        savedBook.title == "The Shining"
        result != null
        result.title == "The Shining"

        when:
        savedBook = bookService.updateBook(result.id, "The Stand").toBlocking().value()
        result = bookService.findWhereTitle("The Stand").toBlocking().first()

        then:
        savedBook != null
        savedBook.title == "The Stand"
        result != null
        result.title == "The Stand"

        when:
        bookService.findWhereTitle("The Shining").toBlocking().first()

        then:
        thrown(NoSuchElementException)

    }
}

@Entity
class Book {
    String title
}


@Service(value = Book, implementers = [FindAndDeleteObservableImplementer, CountByObservableImplementer, CountWhereObservableImplementer, CountObservableImplementer, FindOneObservableImplementer, DeleteObservableImplementer, FindOneByObservableImplementer, FindOneObservableStringQueryImplementer, FindOneObservableWhereImplementer, SaveObservableImplementer, UpdateOneObservableImplementer, UpdateObservableStringQueryImplementer ])
interface BookService {


    @Query("update ${Book b} set $b.title = $title where $b.title = $oldTitle")
    rx.Observable<Number> updateBook(String oldTitle, String title)

    Single<Book> updateBook(Serializable id, String title)

    Single<Book> saveBook(String title)

    @Where({ title ==~ pattern})
    rx.Observable<Book> findWhereTitle(String pattern)

    @Query("from ${Book b} where $b.title = $title")
    rx.Observable<Book> findWithQuery(String title)

    Single<Book> deleteOne(String title)

    Single<Number> delete(String title)

    rx.Observable<Number> count(String title)

    Single<Number> countByTitleLike(String pattern)

    @Where({ title == title})
    Single<Number> countFor(String title)

    rx.Observable<Book> find(String title)

    rx.Observable<Book> findByTitleLike(String pattern)

    Single<Book> findOne(String title)
}

