package org.grails.datastore.gorm.mongodb.boot.autoconfigure

import com.mongodb.Mongo
import grails.mongodb.geo.Point
import grails.persistence.Entity
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Created by graemerocher on 20/03/14.
 */
class MongoDbGormAutoConfigureWithGeoSpacialSpec extends Specification{

    protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    void cleanup() {
        context.close()
    }

    void setup() {

        this.context.register( TestConfiguration, MongoAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
    }


    void 'Test that GORM is correctly configured'() {

        given:"The context is refreshed"
            context.refresh()


        when:"Geospacial data is saved"
            City city
            def location = Point.valueOf([-0.125487, 51.508515])
            City.withTransaction {

                city = new City(name:"London", location: location)
                city.save(flush:true)
                city.discard()

                city = City.get(city.id)
                then:"GORM queries work"
            }
        then:
            city != null
            city.location == location
            City.findByLocationNear(location)
    }

    @Configuration
    @TestAutoConfigurationPackage(City)
    @Import(MongoDbGormAutoConfiguration)
    static class TestConfiguration {
    }

}

@Entity
class City {
    ObjectId id
    String name
    Point location

    static constraints = {
        name blank:false
        location nullable:false
    }

    static mapping = {
        location geoIndex:'2dsphere'
    }
}

