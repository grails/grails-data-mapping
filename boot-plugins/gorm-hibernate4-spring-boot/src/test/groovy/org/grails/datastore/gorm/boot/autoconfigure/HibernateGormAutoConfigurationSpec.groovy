package org.grails.datastore.gorm.boot.autoconfigure

import grails.persistence.Entity
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.HibernateGormEnhancer
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Created by graemerocher on 06/02/14.
 */
class HibernateGormAutoConfigurationSpec extends Specification{

    protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    void cleanup() {
        context.close()
    }

    void setup() {

        this.context.register( TestConfiguration, EmbeddedDataSourceConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
    }


    void 'Test that GORM is correctly configured'() {
        when:"The context is refreshed"
            context.refresh()


        then:"GORM queries work"
            Person.count() == 0
            Person.list().size() == 0
    }

    @Configuration
    @TestAutoConfigurationPackage(Person)
    @Import(HibernateGormAutoConfiguration)
    static class TestConfiguration {
    }

}


@Entity
class Person {
    String firstName
    String lastName
    Integer age = 18
}
