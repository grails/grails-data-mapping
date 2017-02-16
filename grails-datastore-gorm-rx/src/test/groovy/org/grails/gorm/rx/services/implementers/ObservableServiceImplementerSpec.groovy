package org.grails.gorm.rx.services.implementers

import grails.gorm.annotation.Entity
import grails.gorm.services.Service
import org.grails.gorm.rx.services.support.RxServiceSupport
import rx.Observable
import rx.Single
import spock.lang.Specification

/**
 * Created by graemerocher on 15/02/2017.
 */
class ObservableServiceImplementerSpec extends Specification {

    void "test find method that returns an Observable"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.services.Service
import rx.Observable

@Entity
class Book {
  String title
}
@Service(value=Book,implementers = [org.grails.gorm.rx.services.implementers.FindOneObservableImplementer])
interface BookService {
    Observable<Book> find(String title)
}
return Book.classLoader
''')
        println cl.loadedClasses

        when:
        Class impl = cl.loadClass('$BookServiceImplementation')
        then:
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:"A method is invoked that returns an observable"
        def instance = impl.newInstance()
        Observable o = instance.find("test")

        then:"an observable is returned"
        o != null

        when:"The observable result is produced"
        def result = o.toBlocking().first()

        then:"A GORM method is invoked"
        def e = thrown(IllegalStateException)
        e.message.startsWith('No GORM implementations configured')
    }

    void "test count @Where method that returns an Observable"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.services.Service
import grails.gorm.services.Where
import rx.Observable
import org.grails.gorm.rx.services.implementers.*
import rx.Single

@Entity
class Book {
  String title
}
@Service(value=Book,implementers = [CountWhereObservableImplementer])
interface BookService {
    @Where({ title == tit})
    Single<Number> count(String tit)
}
return Book.classLoader
''')
        println cl.loadedClasses

        when:
        Class impl = cl.loadClass('$BookServiceImplementation')
        then:
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:"A method is invoked that returns an observable"
        def instance = impl.newInstance()
        Single o = instance.count("test")

        then:"an observable is returned"
        o != null

        when:"The observable result is produced"
        def result = o.toBlocking().value()

        then:"A GORM method is invoked"
        def e = thrown(IllegalStateException)
        e.message.startsWith('No GORM implementations configured')
    }
}

//@Entity
//class Book {
//  String title
//}
//
//
//@Service(Book)
//abstract class BookService {
//    abstract Observable<Book> find(String title)
//
//    Observable<Book> findOne(title) {
//        RxServiceSupport.create {
//            Book.findWhere(title:title)
//        }
//    }
//}
