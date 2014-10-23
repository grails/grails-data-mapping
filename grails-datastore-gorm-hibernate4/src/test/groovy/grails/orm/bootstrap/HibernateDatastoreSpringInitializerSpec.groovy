package grails.orm.bootstrap

import grails.persistence.Entity
import org.h2.Driver
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
            def dataSource = new DriverManagerDataSource(Driver.name, "jdbc:h2:mem:grailsDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')
            applicationContext.beanFactory.registerSingleton("dataSource", dataSource)


        when:"The application context is configured"
            datastoreInitializer.configureForBeanDefinitionRegistry(applicationContext)
            applicationContext.refresh()
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
            if(applicationContext.isRunning()) {
                applicationContext.stop()
            }

    }

    void "Test that GORM is initialized correctly for a DataSource"() {
        given:"An initializer instance"

            def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person)
            def dataSource = new DriverManagerDataSource(Driver.name, "jdbc:h2:mem:grailsDb2;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1", 'sa', '')

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
