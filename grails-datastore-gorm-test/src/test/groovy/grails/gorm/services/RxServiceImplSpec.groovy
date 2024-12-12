package grails.gorm.services

import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.services.Implemented
import org.grails.datastore.gorm.services.implementers.DeleteImplementer
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import io.reactivex.rxjava3.core.Single
import spock.lang.AutoCleanup
import spock.lang.PendingFeature
import spock.lang.Specification

/**
 * Created by graemerocher on 15/02/2017.
 */
class RxServiceImplSpec extends Specification {
    @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
        Book
    )

//    @PendingFeature(reason="bookService.countByTitleLike(The%).toBlocking().value() == 1")
    void "test find method that returns an observable"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        expect:"the observable returns the correct res"

        bookService.countByTitleLike("The%").blockingGet() as Integer == 1
        bookService.countFor("The Stand").blockingGet() as Integer == 1
        bookService.count("The Stand").blockingGet() as Integer == 1
        bookService.find("The Stand").toList().blockingGet().size() == 1
        bookService.findOne("The Stand").blockingGet().title == "The Stand"
        bookService.findByTitleLike("The%").blockingFirst().title == "The Stand"
    }

//    @PendingFeature(reason="org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object '1' with class 'java.lang.Long' to class 'io.reactivex.rxjava3.core.Single'")
    void "test delete method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)


        when:
        def result = bookService.delete("The Stand").blockingGet()
        def implementer = bookService.getClass().getMethod("delete", String).getAnnotation(Implemented).by()
        then:
        implementer == DeleteImplementer
        result == 1

        when:
        bookService.find("The Stand").blockingFirst() == null

        then:
        thrown(NoSuchElementException)

    }

//    @PendingFeature(reason="org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object 'grails.gorm.services.Book : 1' with class 'grails.gorm.services.Book' to class 'io.reactivex.rxjava3.core.Single'")
    void "test find and delete method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        Book result = bookService.deleteOne("The Stand").blockingGet()

        then:
        result != null
        result.title == "The Stand"

        when:
        bookService.find("The Stand").blockingFirst() == null

        then:
        thrown(NoSuchElementException)

    }

//    @PendingFeature(reason="Expected exception of type 'java.lang.UnsupportedOperationException', but got 'groovy.lang.MissingMethodException'")
    void "test find with string query method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        def result = bookService.findWithQuery("The Stand").blockingFirst()

        then:
        thrown(UnsupportedOperationException)

        when:
        bookService.updateBook("The Stand", "It").blockingFirst()

        then:
        thrown(UnsupportedOperationException)

    }

//    @PendingFeature(reason="org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object '[grails.gorm.services.Book : 2]' with class 'java.util.ArrayList' to class 'io.reactivex.rxjava3.core.Observable' due to: groovy.lang.GroovyRuntimeException: Could not find matching constructor for: io.reactivex.rxjava3.core.Observable(grails.gorm.services.Book)")
    void "test find with where query method"() {
        given:
        new Book(title: "The Stand").save(flush:true)
        new Book(title: "The Shining").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        Book result = bookService.findWhereTitle("The Shining").blockingFirst()

        then:
        result != null
        result.title == "The Shining"

    }

//    @PendingFeature(reason="org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object 'grails.gorm.services.Book : 1' with class 'grails.gorm.services.Book' to class 'io.reactivex.rxjava3.core.Single'")
    void "test save method"() {
        given:
        BookService bookService = datastore.getService(BookService)

        when:
        Book savedBook = bookService.saveBook("The Shining").blockingGet()
        Book result = bookService.findWhereTitle("The Shining").blockingFirst()


        then:
        savedBook != null
        savedBook.title == "The Shining"
        result != null
        result.title == "The Shining"

        when:
        savedBook = bookService.updateBook(result.id, "The Stand").blockingGet()
        result = bookService.findWhereTitle("The Stand").blockingFirst()

        then:
        savedBook != null
        savedBook.title == "The Stand"
        result != null
        result.title == "The Stand"

        when:
        bookService.findWhereTitle("The Shining").blockingFirst()

        then:
        thrown(NoSuchElementException)

    }

//    @PendingFeature(reason="groovy.lang.MissingMethodException: No signature of method: grails.gorm.services.BookServiceImplementation.findBookAuthor() is applicable for argument types: (String) values: [The Stand]")
    void "test simple projection"() {

        given:
        new Book(title: "Along Came a Spider", author: "James Patterson").save(flush:true)
        new Book(title: "The Stand", author: "Stephen King").save(flush:true)
        BookService bookService = datastore.getService(BookService)

        when:
        String author = bookService.findBookAuthor("The Stand").blockingGet()

        then:
        author == "Stephen King"
    }
}

@Entity
class Book {
    String title
    String author
}


@Service(value = Book)
interface BookService {

//    Cannot implement method for argument [title]. No property exists on domain class [java.lang.String]
    //Single<String> findBookAuthor(String title)

//    No implementations possible for method 'io.reactivex.rxjava3.core.Observable updateBook(java.lang.String, java.lang.String)'. Please use an abstract class instead and provide an implementation.
    //@Query("update ${Book b} set $b.title = $title where $b.title = $oldTitle")

//    No implementations possible for method 'io.reactivex.rxjava3.core.Observable updateBook(java.lang.String, java.lang.String)'. Please use an abstract class instead and provide an implementation.
    //io.reactivex.rxjava3.core.Observable<Number> updateBook(String oldTitle, String title)

    Single<Book> updateBook(Serializable id, String title)

    Single<Book> saveBook(String title)

    @Where({ title ==~ pattern})
    io.reactivex.rxjava3.core.Observable<Book> findWhereTitle(String pattern)

    @Query("from ${Book b} where $b.title = $title")
    io.reactivex.rxjava3.core.Observable<Book> findWithQuery(String title)

    Single<Book> deleteOne(String title)

    Single<Number> delete(String title)

    Single<Number> count(String title)

    Single<Number> countByTitleLike(String pattern)

    @Where({ title == title})
    Single<Number> countFor(String title)

    io.reactivex.rxjava3.core.Observable<Book> find(String title)

    io.reactivex.rxjava3.core.Observable<Book> findByTitleLike(String pattern)

    Single<Book> findOne(String title)
}

