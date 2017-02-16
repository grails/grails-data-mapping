package grails.gorm.services

import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.services.implementers.CountWhereImplementer
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.gorm.rx.services.implementers.CountByObservableImplementer
import org.grails.gorm.rx.services.implementers.CountObservableImplementer
import org.grails.gorm.rx.services.implementers.CountWhereObservableImplementer
import org.grails.gorm.rx.services.implementers.DeleteObservableImplementer
import org.grails.gorm.rx.services.implementers.FindOneObservableImplementer
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
}

@Entity
class Book {
    String title
}


@Service(value = Book, implementers = [CountByObservableImplementer, CountWhereObservableImplementer, CountObservableImplementer, FindOneObservableImplementer, DeleteObservableImplementer ])
interface BookService {

    Single<Number> delete(String title)

    rx.Observable<Number> count(String title)

    Single<Number> countByTitleLike(String pattern)

    @Where({ title == title})
    Single<Number> countFor(String title)

    rx.Observable<Book> find(String title)

    Single<Book> findOne(String title)
}

