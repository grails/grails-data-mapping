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

    void "Test that GORM is initialized correctly"() {
       given:"An initializer instance"

            def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person)
            def applicationContext = new GenericApplicationContext()
            def dataSource = new DriverManagerDataSource(Driver.name, "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE", 'sa', '')
            applicationContext.beanFactory.registerSingleton("dataSource", dataSource)

        when:"The application context is configured"
            datastoreInitializer.configure(applicationContext)
            applicationContext.refresh()
            def conn = dataSource.getConnection()

        then:"The database tables are created correctly"
            conn.prepareStatement("SELECT * FROM PERSON").execute()

        when:"A GORM method is invoked"
            def total = Person.count()

        then:"The correct results are returned"
            total == 0

        when:"A new domain instance is created"
            def p = new Person()

        then:"it is initially invalid"
            !p.validate()

        when:"it is made valid"
            p.name = "Bob"

        then:"It can be saved"
            p.save(flush:true)
            Person.count() == 1



    }
}
@Entity
class Person {
    String name

    static constraints = {
        name blank:false
    }
}
