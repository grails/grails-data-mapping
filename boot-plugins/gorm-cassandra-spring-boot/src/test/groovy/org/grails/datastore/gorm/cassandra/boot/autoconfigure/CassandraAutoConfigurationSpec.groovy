package org.grails.datastore.gorm.cassandra.boot.autoconfigure

import grails.persistence.Entity
import org.grails.boot.internal.EnableAutoConfiguration
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Created by graemerocher on 23/11/15.
 */
class CassandraAutoConfigurationSpec extends Specification{

    protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    void cleanup() {
        context.close()
    }

    void setup() {

        System.setProperty(CassandraDatastore.KEYSPACE_NAME, "test")
        System.setProperty(CassandraDatastore.KEYSPACE_ACTION, "create-drop")
        System.setProperty(CassandraDatastore.SCHEMA_ACTION, 'recreate-drop-unused')
        AutoConfigurationPackages.register(context, CassandraAutoConfiguration.package.name)
        this.context.register( TestConfiguration, PropertyPlaceholderAutoConfiguration.class);
    }


    void 'Test that GORM is correctly configured'() {
        when:"The context is refreshed"
        context.refresh()

        then:"GORM queries work"
        Person.count() == 0
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(CassandraAutoConfiguration)
    static class TestConfiguration {
    }

}


@Entity
class Person {
    String firstName
    String lastName
    Integer age = 18
}


