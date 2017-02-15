package org.grails.gorm.rx.services.implementers

import grails.gorm.annotation.Entity
import grails.gorm.services.Service
import rx.Observable
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
@Service(Book)
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
}

@Entity
class Book {
  String title
}


@Service(Book)
interface BookService {
    Observable<Book> find(String title)
}
