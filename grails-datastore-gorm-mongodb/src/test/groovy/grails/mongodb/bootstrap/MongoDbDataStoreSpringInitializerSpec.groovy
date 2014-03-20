package grails.mongodb.bootstrap

import com.mongodb.Mongo
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