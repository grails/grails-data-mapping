package org.grails.datastore.gorm.boot.autoconfigure

import grails.persistence.Entity
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
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

        AutoConfigurationPackages.register(context, "org.grails.datastore.gorm.boot.autoconfigure")
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
