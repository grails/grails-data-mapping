package grails.mongodb.bootstrap

import com.mongodb.Mongo
import grails.mongodb.geo.Point
import grails.persistence.Entity
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class MongoDbDataStoreSpringInitializerSpec extends Specification{

    void "Test that MongoDbDatastoreSpringInitializer can setup GORM for MongoDB from scratch"() {
        when:"the initializer used to setup GORM for MongoDB"
            def initializer = new MongoDbDataStoreSpringInitializer(Person)
            def applicationContext = initializer.configure()
            def mongo = applicationContext.getBean(Mongo)
            mongo.getDB(MongoDbDataStoreSpringInitializer.DEFAULT_DATABASE_NAME).dropDatabase()

        then:"GORM for MongoDB is initialized correctly"
            Person.count() == 0

    }

    @Issue('GPMONGODB-339')
    void "Test withTransaction returns connections when used without session handling"() {
        given:"the initializer used to setup GORM for MongoDB"
            def initializer = new MongoDbDataStoreSpringInitializer(Person)
            def applicationContext = initializer.configure()
            def mongo = applicationContext.getBean(Mongo)

        when:"The a normal GORM method is used"
            Person.count()
        then:"No connections are in use afterwards"
            mongo.connector.@_masterPortPool.statistics.inUse == 0

        when:"The withTransaction method is used"
            Person.withTransaction {
                new Person(name:"Bob").save()
            }

        then:"No connections in use"
            mongo.connector.@_masterPortPool.statistics.inUse == 0
    }

    void "Test that constraints and Geo types work"() {
        given:"the initializer used to setup GORM for MongoDB"
            def initializer = new MongoDbDataStoreSpringInitializer(Person)
            def applicationContext = initializer.configure()
            def mongo = applicationContext.getBean(Mongo)

        when:"we try to persist an invalid object"
            def p = new Person().save(flush:true)

        then:"The object is null and not persisted"
            p == null
            Person.count() == 0

        when:"We persist a Geo type"
            new Person(name: "Bob", home: Point.valueOf(10, 10)).save(flush:true)
            p = Person.first()

        then:"The geo type was persisted"
            p != null
            p.home != null

    }
}


@Entity
class Person {
    Long id
    Long version
    String name
    Point home

    static constraints = {
        name blank:false
    }
}