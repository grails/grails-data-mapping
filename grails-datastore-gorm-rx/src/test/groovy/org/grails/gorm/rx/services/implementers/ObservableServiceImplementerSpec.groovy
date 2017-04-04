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
    void "test dynamic finder method that returns an Observable"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.rx.services.RxSchedule
import grails.gorm.services.Service
import rx.Observable
import rx.schedulers.Schedulers
import org.grails.gorm.rx.services.implementers.*

@Entity
class Book {
  String title
}
@Service(value=Book, adapters = [ObservableServiceImplementerAdapter])
interface BookService {
    Observable<Book> findByTitleLike(String title)
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
        Observable o = instance.findByTitleLike("test")

        then:"an observable is returned"
        o != null

        when:"The observable result is produced"
        def result = o.toBlocking().first()

        then:"A GORM method is invoked"
        def e = thrown(IllegalStateException)
        e.message.startsWith('No GORM implementations configured')
    }

    void "test find method that returns an Observable"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.rx.services.RxSchedule
import grails.gorm.services.Service
import rx.Observable
import rx.schedulers.Schedulers
import org.grails.gorm.rx.services.implementers.*

@Entity
class Book {
  String title
}
@Service(value=Book, adapters = [ObservableServiceImplementerAdapter])
interface BookService {
    @RxSchedule(scheduler = { Schedulers.newThread() })
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

    void "test find @Where method that returns an Observable"() {
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
@Service(value=Book,adapters= [ObservableServiceImplementerAdapter])
interface BookService {
    @Where({ title ==~ pattern})
    rx.Observable<Book> findWhereTitle(String pattern)
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
        Observable o = instance.findWhereTitle("test")

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
@Service(value=Book,adapters= [ObservableServiceImplementerAdapter])
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

    void "test update with @Query annotation"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.services.Query
import grails.gorm.services.Service
import grails.gorm.services.Where
import rx.Observable
import org.grails.gorm.rx.services.implementers.*
import rx.Single

@Entity
class Book {
  String title
}
@Service(value=Book,adapters= [ObservableServiceImplementerAdapter])
interface BookService {
    @Query("update ${Book b} set $b.title = $title where $b.title = $oldTitle")
    Single<Number> updateBook(String oldTitle, String title)
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
        Single o = instance.updateBook("test", "test2")

        then:"an observable is returned"
        o != null

        when:"The observable result is produced"
        def result = o.toBlocking().value()

        then:"A GORM method is invoked"
        def e = thrown(IllegalStateException)
        e.message.startsWith('No GORM implementations configured')
    }

    void "test property projection"() {
        given:
        ClassLoader cl = new GroovyShell().evaluate('''
import grails.gorm.annotation.Entity
import grails.gorm.services.Query
import grails.gorm.services.Service
import grails.gorm.services.Where
import rx.Observable
import org.grails.gorm.rx.services.implementers.*
import rx.Single

@Entity
class Book {
  String title
  String author
}
@Service(value=Book,adapters= [ObservableServiceImplementerAdapter])
interface BookService {
    Single<String> findBookAuthor(String title)
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
        Single o = instance.findBookAuthor("test")

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
