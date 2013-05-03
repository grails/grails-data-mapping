package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class MongoDynamicPropertyOnEmbeddedSpec extends GormDatastoreSpec{

    @Issue('GPMONGODB-290')
    void "Test that accessing dynamic attributes on embedded objects use the embedded collection"() {
        when:"An embedded collection is created"
            Container.collection.insert(name:'big box of items',
                    contents:(0..9).collect { [name:"Item $it"]})

        then:"The embedded collection is valid"
            Container.count() == 1
            Container.first().contents.size() == 10
            Container.first().contents.first().name ==~ /Item \d/
            Container.collection.DB.getCollectionNames().contains "container"
            !Container.collection.DB.getCollectionNames().contains( "item" )
            session.clear()

        when:"An embedded dynamic property is accessed"
            Container.first().contents.first().nonexistentProperty == null
        then:"The a collection is not created for the embedded property"
            Container.collection.DB.getCollectionNames().contains "container"
            !Container.collection.DB.getCollectionNames().contains( "item" )

    }

    @Override
    List getDomainClasses() {
        [Container]
    }
}

@Entity
class Container {
    static mapWith='mongo'
    static embedded = ['contents']
    static mapping = {
        version false
        cache false
    }
    static constraints = {
        contents nullable:true
    }


    ObjectId id
    String name

    Set<Item> contents = new HashSet<Item>()
}
@Entity
class Item {
    static mapWith='mongo'
    static mapping = {
        version false
    }
    static constraints = {

    }
    static belongsTo = Container

    ObjectId id
    String name


}