package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import spock.lang.Issue

/**
 */
class BeforeUpdateEventSpec extends GormDatastoreSpec {

    @Issue('GRAILS-8916')
    void "Test beforeUpdate event doesn't cause test failure"() {
        when:"An entity is saved that has a beforeUpdate event"
            BeforeUpdateBook b = new BeforeUpdateBook()
            b.save(failOnError:true)
            BeforeUpdateAuthor a = new BeforeUpdateAuthor()
            a.save(failOnError:true)

            a.book = b
            a.save(failOnError:true)

        then:"The association index is persisted correctly"
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

