package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class BasicCollectionsSpec extends GormDatastoreSpec{
    @Override
    List getDomainClasses() {
        [Linguist, Increment]
    }

    void "Test that a Locale can be used inside a collection"() {
        when:"A locale collection is persisted"
        def p = new Linguist(name:"Bob")
        p.spokenLanguages << Locale.UK << Locale.CANADA_FRENCH << Locale.US
        p.save(flush:true)

        then:"The collection is still ok"
        p.spokenLanguages == [Locale.UK, Locale.CANADA_FRENCH, Locale.US]

        when:"The entity is refetched"
        session.clear()
        p = Linguist.get(p.id)

        then:"The embedded collection and locales can be read back correctly"
        p.name == "Bob"
        p.spokenLanguages == [Locale.UK, Locale.CANADA_FRENCH, Locale.US]
    }

    void "Test that a map of Currency works."() {
        when:"A currency map is persisted"
        def p = new Linguist(name:"Bob")
        p.currencies.put(Locale.UK.toString(), Currency.getInstance("GBP"))
        p.currencies.put(Locale.US.toString(), Currency.getInstance("USD"))
        p.save(flush:true)

        then:"The collection is still ok"
        p.currencies == [
                (Locale.UK.toString()):Currency.getInstance("GBP"),
                (Locale.US.toString()):Currency.getInstance("USD")
        ]

        when:
        session.clear()
        p = Linguist.get(p.id)

        then:"The embedded collection and locales can be read back correctly"
        p.name == "Bob"
        p.currencies == [
                (Locale.UK.toString()):Currency.getInstance("GBP"),
                (Locale.US.toString()):Currency.getInstance("USD")
        ]
    }

    void "Test beforeInsert() and beforeUpdate() methods for collections"() {
        when:"An entity is persisted"
        def p = new Increment()
        p.save(flush:true)
        session.clear()
        p = Increment.get(p.id)

        then:"The collection is updated"
        p.counter == 1
        p.history == [0]

        when:"The entity is updated"
        p.counter = 10
        p.save(flush:true)
        session.clear()
        p = Increment.get(p.id)

        then:"The collection is updated too"
        p.counter == 11
        p.history == [0, 10]
    }
}

@Entity
class Linguist {
    String id
    String name
    List<Locale> spokenLanguages = []
    Map<String, Currency> currencies = [:]

    static constraints = {
    }
}

@Entity
class Increment {
    String id
    Integer counter = 0
    List<Integer> history = []

    def beforeInsert() {
        inc()
    }

    def beforeUpdate() {
        inc()
    }

    def inc() {
        history << counter++
    }
}