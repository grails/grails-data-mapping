package grails.orm.bootstrap

import grails.persistence.Entity
import org.h2.Driver
import org.hibernate.Session
import org.hibernate.dialect.H2Dialect
import org.springframework.context.support.GenericApplicationContext
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.util.Log4jConfigurer
import spock.lang.Specification

/**
 * Created by graemerocher on 29/01/14.
 */
class HibernateDatastoreSpringInitializerSpec extends Specification{

    void "Test that GORM is initialized correctly for an existing BeanDefinitionRegistry"() {
        given:"An initializer instance"

        def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person)
        def applicationContext = new GenericApplicationContext()
        def dataSource = new DriverManagerDataSource("jdbc:h2:mem:grailsDb1;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        dataSource.driverClassName = Driver.name
        applicationContext.beanFactory.registerSingleton("dataSource", dataSource)


        when:"The application context is configured"
        datastoreInitializer.configureForBeanDefinitionRegistry(applicationContext)
        boolean refreshCalled = false
        applicationContext.refresh()
        refreshCalled = true
        def conn = dataSource.getConnection()

        then:"The database tables are created correctly"
        conn.prepareStatement("SELECT * FROM PERSON").execute()

        when:"A GORM method is invoked"
        def total = Person.withNewSession { Person.count() }

        then:"The correct results are returned"
        total == 0

        when:"A new domain instance is created"
        def p = new Person()

        then:"it is initially invalid"
        ! Person.withNewSession { p.validate() }

        when:"it is made valid"
        p.name = "Bob"

        then:"It can be saved"
        Person.withNewSession { p.save(flush:true) }
        Person.withNewSession { Person.count() } == 1


        cleanup:
        if(refreshCalled && applicationContext.isRunning()) {
            applicationContext.stop()
        }

    }

    void "Test that GORM is initialized correctly for a DataSource"() {
        given:"An initializer instance"

        def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person)
        def dataSource = new DriverManagerDataSource("jdbc:h2:mem:grailsDb2;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        dataSource.driverClassName = Driver.name


        when:"The application context is configured"
        datastoreInitializer.configureForDataSource(dataSource)
        def conn = dataSource.getConnection()

        then:"The database tables are created correctly"
        conn.prepareStatement("SELECT * FROM PERSON").execute()

        when:"A GORM method is invoked"
        def total = Person.withNewSession { Person.count() }

        then:"The correct results are returned"
        total == 0

        when:"A new domain instance is created"
        def p = new Person()

        then:"it is initially invalid"
        !Person.withNewSession { p.validate() }

        when:"it is made valid"
        p.name = "Bob"

        then:"It can be saved"
        Person.withNewSession { p.save(flush:true) }
        Person.withNewSession { Person.count()  } == 1



    }

    void "Test configure multiple data sources"() {
        given:"An initializer instance"

        def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person, Book, Author)
        def dataSource = new DriverManagerDataSource("jdbc:h2:mem:people;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        dataSource.driverClassName = Driver.name

        def booksDs = new DriverManagerDataSource("jdbc:h2:mem:books;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        booksDs.driverClassName = Driver.name

        def moreBooksDs = new DriverManagerDataSource("jdbc:h2:mem:moreBooks;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        moreBooksDs.driverClassName = Driver.name

        when:"the application is configured"
        datastoreInitializer.configureForDataSources(dataSource: dataSource, books:booksDs, moreBooks:moreBooksDs)

        then:"Each domain has the correct data source(s)"
        Person.count() == 0
        Person.withSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:people"
            return true
        }
        Book.count() == 0
        Book.withSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:books"
            return true
        }
        Book.books.count() == 0
        Book.books.withSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:books"
            return true
        }
        Book.moreBooks.count() == 0
        Book.moreBooks.withNewSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:moreBooks"
            return true
        }
        Author.withNewSession { Author.count() == 0 }
        Author.withNewSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:people"
            return true
        }
        Author.books.withNewSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:books"
            return true
        }
        Author.moreBooks.withNewSession { Session s ->
            assert s.connection().metaData.getURL() == "jdbc:h2:mem:moreBooks"
            return true
        }

    }

    void "Test configure custom dialect with a Class"() {
        given:"An initializer instance"

        def datastoreInitializer = new HibernateDatastoreSpringInitializer(['dataSource.dialect': H2Dialect], Person)
        def dataSource = new DriverManagerDataSource("jdbc:h2:mem:dialectTest;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        dataSource.driverClassName = Driver.name


        when:"The application context is configured"
        datastoreInitializer.configureForDataSource(dataSource)
        def conn = dataSource.getConnection()

        then:"The database tables are created correctly"
        conn.prepareStatement("SELECT * FROM PERSON").execute()

        when:"A GORM method is invoked"
        def total = Person.withNewSession { Person.count() }

        then:"The correct results are returned"
        total == 0

        when:"A new domain instance is created"
        def p = new Person()

        then:"it is initially invalid"
        !Person.withNewSession { p.validate() }

        when:"it is made valid"
        p.name = "Bob"

        then:"It can be saved"
        Person.withNewSession { p.save(flush:true) }
        Person.withNewSession { Person.count()  } == 1

    }

    void "Test that supplying default constraints works as expected"() {

        given:"an initializer with default constraints supplied"

        def initializer = new HibernateDatastoreSpringInitializer(['grails.gorm.default.constraints': {
            '*'(nullable: true, blank: true)
        }], DefaultConstrainedEntity)

        def dataSource = new DriverManagerDataSource("jdbc:h2:mem:dialectTest;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
        dataSource.driverClassName = Driver.name
        initializer.configureForDataSource(dataSource)

        when:"The entity is saved"

        def obj = new DefaultConstrainedEntity()
        obj.save(flush:true)

        then:"no constraints are violated"
        !obj.errors.hasErrors()
        DefaultConstrainedEntity.count() == 1
    }
}

@Entity
class DefaultConstrainedEntity {
    String name
    String otherValue

    static mapping = {
        otherValue sqlType: "text"
    }
}
@Entity
class Person {
    Long id
    Long version
    String name

    static constraints = {
        name blank:false
    }
}

@Entity
class Book {
    Long id
    Long version
    String name

    static mapping = {
        datasources( ['books', 'moreBooks'] )
    }
    static constraints = {
        name blank:false
    }
}

@Entity
class Author {
    Long id
    Long version
    String name

    static mapping = {
        datasource 'ALL'
    }
    static constraints = {
        name blank:false
    }
}
