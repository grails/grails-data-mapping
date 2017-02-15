package grails.gorm.services

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.gorm.rx.services.implementers.FindOneObservableImplementer
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
        bookService.find("The Stand").toList().toBlocking().single().size() == 1
    }
}

@Entity
class Book {
    String title
}


@Service(value = Book, implementers = FindOneObservableImplementer)
interface BookService {
    rx.Observable<Book> find(String title)
}

