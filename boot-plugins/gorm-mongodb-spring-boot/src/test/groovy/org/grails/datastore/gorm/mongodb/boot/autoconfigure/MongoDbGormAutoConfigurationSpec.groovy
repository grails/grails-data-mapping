package org.grails.datastore.gorm.mongodb.boot.autoconfigure

import grails.persistence.Entity
import org.grails.boot.internal.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Tests for MongoDB autoconfigure
 */
class MongoDbGormAutoConfigurationSpec extends Specification{

    protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    void cleanup() {
        context.close()
    }

    void setup() {

        AutoConfigurationPackages.register(context, "org.grails.datastore.gorm.mongodb.boot.autoconfigure")
        this.context.register( TestConfiguration, MongoAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
    }


    void 'Test that GORM is correctly configured'() {
        when:"The context is refreshed"
            context.refresh()

        then:"GORM queries work"
            Person.count() != null
    }

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan("org.grails.datastore.gorm.mongodb.boot.autoconfigure")
    @Import(MongoDbGormAutoConfiguration)
    static class TestConfiguration {
    }

}


@Entity
class Person {
    String firstName
    String lastName
    Integer age = 18
}

