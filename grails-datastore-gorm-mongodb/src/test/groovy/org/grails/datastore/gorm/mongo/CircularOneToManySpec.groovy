package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class CircularOneToManySpec extends GormDatastoreSpec {

    @Issue('GPMONGODB-254')
    void "Test store and retrieve circular one-to-many association"() {
        given:"A circular one-to-many"
            new Profile(name: "Fred")
                 .addToFriends(name: "Bob")
                 .addToFriends(name: "Frank")
                 .save(flush:true)

            session.clear()

        when:"The entity is loaded"
            def fred = Profile.get(1L)

        then:"The association is valid"
            fred.name == "Fred"
            fred.friends.size() == 2
            fred.friends.any { it.name == "Bob" }
            fred.friends.any { it.name == "Frank" }

    }

    @Override
    List getDomainClasses() {
        [Profile]
    }
}

@Entity
class Profile {
    Long id
    String name
    List<Profile> friends

    static hasMany = [friends:Profile]
}