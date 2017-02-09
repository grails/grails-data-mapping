package grails.gorm.services

import grails.gorm.annotation.Entity
import grails.gorm.multitenancy.TenantService
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.TransactionService
import grails.gorm.transactions.Transactional
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.services.DefaultServiceRegistry
import org.grails.datastore.mapping.services.ServiceRegistry
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * Created by graemerocher on 11/01/2017.
 */
class ServiceTransformSpec extends Specification {

    void "test implement abstract class"() {
        when:"The service transform is applied to an abstract class"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
abstract class AbstractMyService implements MyService {

    Foo readFoo(Serializable id) {
        Foo.read(id)
    }
    
    @Override
    Foo delete(Serializable id) {
        def foo = Foo.get(id)
        foo?.delete()
        foo?.title = "DELETED"
        return foo
    }
}
 
interface MyService {
    Number deleteMoreFoos(String title)
    
    void deleteFoos(String title)
//    Foo get(Serializable id)
    
    Foo delete(Serializable id)
    
    List<Foo> listFoos()
    
    Foo[] listMoreFoos()
    
    Iterable<Foo> listEvenMoreFoos()
    
    List<Foo> findByTitle(String title)
    
    List<Foo> findByTitleLike(String title)
    
    Foo saveFoo(String title)
}
@Entity
class Foo {
    String title
}

''')
        then:
        !service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$AbstractMyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("deleteMoreFoos", String).getAnnotation(Transactional) != null
        impl.getMethod("delete", Serializable).getAnnotation(Transactional) != null
        impl.getMethod("readFoo", Serializable).getAnnotation(ReadOnly) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" } != null
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:
        impl.newInstance().listFoos()

        then:
        def e = thrown(IllegalStateException)
        e.message == 'No GORM implementations configured. Ensure GORM has been initialized correctly'
    }

    void "test implement list method"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(expose=false, value=Foo)
interface MyService {
    Number deleteMoreFoos(String title)
    
    void deleteFoos(String title)
//    Foo get(Serializable id)
    
    Foo delete(Serializable id)
    
    List<Foo> listFoos()
    
    Foo[] listMoreFoos()
    
    Iterable<Foo> listEvenMoreFoos()
    
    List<Foo> findByTitle(String title)
    
    List<Foo> findByTitleLike(String title)
    
    Foo saveFoo(String title)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("deleteMoreFoos", String).getAnnotation(Transactional) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" } != null
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:
        impl.newInstance().listFoos()

        then:
        def e = thrown(IllegalStateException)
        e.message == 'No GORM implementations configured. Ensure GORM has been initialized correctly'
    }

    void "test service transform applied to interface that can't be implemented"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*

@Service
interface MyService {
    void foo()
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''No implementations possible for method 'void foo()'. Please use an abstract class instead and provide an implementation.
 @ line 6, column 5.
       void foo()
       ^'''
    }

    void "test service transform applied with a dynamic finder for a non-existent property"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(expose=false)
interface MyService {
    
    List<Foo> findByTitLike(String title)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''Cannot implement finder for non-existent property [tit] of class [Foo]
 @ line 8, column 5.
       List<Foo> findByTitLike(String title)'''
    }
    void "test service transform applied with a dynamic finder for a property of the wrong type"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(expose=false)
interface MyService {
    
    List<Foo> find(Integer title)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''Cannot implement method for argument [title]. No property exists on domain class [Foo]
 @ line 8, column 5.
       List<Foo> find(Integer title)
       ^'''
    }

    void "test service transform"() {
        given:
        ServiceRegistry reg = new DefaultServiceRegistry(Mock(Datastore), false)

        expect:
        org.grails.datastore.mapping.services.Service.isAssignableFrom(TestService)
        reg.getService(TestService) != null
        reg.getService(TestService2) != null
        reg.getService(TestService).datastore != null
        reg.getService(TransactionService) != null
        reg.getService(TenantService) != null
    }
}

@Service
class TestService {
    void doStuff() {
    }
}

@Service
class TestService2 {
    void doStuff() {
    }
}
