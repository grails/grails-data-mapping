package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

/**
 */
class BeforeUpdateEventSpec extends GormDatastoreSpec {

    void "Test beforeUpdate event doesn't cause test failure"() {
        when:"blah"
            BeforeUpdateBook b = new BeforeUpdateBook()
            b.save(failOnError:true)
            BeforeUpdateAuthor a = new BeforeUpdateAuthor()
            a.save(failOnError:true)

            a.book = b
            a.save(failOnError:true)

        then:"blah"
            a.id == BeforeUpdateAuthor.findByBook(b).id

    }

    @Override
    List getDomainClasses() {
        [BeforeUpdateAuthor, BeforeUpdateBook]
    }
}

@Entity
class BeforeUpdateBook {
    Long id
    static hasMany = [authors:BeforeUpdateAuthor]

    static constraints = {
    }
}

@Entity
class BeforeUpdateAuthor {
    Long id
    BeforeUpdateBook book

    def beforeUpdate(){}

    static constraints = {
        book(nullable:true)
    }
}

