package grails.gorm.tests

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.springframework.datastore.mapping.core.Session

/**
 * @author graemerocher
 */
class DomainEventsSpec extends GormDatastoreSpec {

    Session setupEventsSession() {
        def datastore = session.datastore
        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)
        session = datastore.connect()
    }

    void "Test modify property before save"() {
        given:
            session = setupEventsSession()
            session.datastore.mappingContext.addPersistentEntity(ModifyPerson)
            def p = new ModifyPerson(name:"Bob").save(flush:true)
            session.clear()

        when:
            p = ModifyPerson.get(p.id)

        then:
            p.name == "Fred"
    }

    void "Test auto time stamping working"() {

        given:
            session = setupEventsSession()

            def p = new PersonEvent()

            p.name = "Fred"
            p.save(flush:true)
            session.clear()

        when:
            p = PersonEvent.get(p.id)

        then:
            sleep(2000)

            p.dateCreated == p.lastUpdated

        when:
            p.name = "Wilma"
            p.save(flush:true)

        then:
            p.dateCreated.before(p.lastUpdated) == true
    }

//    void testOnloadEvent() {
//        def personClass = ga.getDomainClass("PersonEvent")
//        def p = personClass.newInstance()
//
//        p.name = "Fred"
//        p.save()
//        session.flush()
//        session.clear()
//
//        p = personClass.clazz.get(1)
//        assertEquals "Bob", p.name
//    }

    void "Test before delete event"() {
        given:
            session = setupEventsSession()
            PersonEvent.resetStore()
            def p = new PersonEvent()
            p.name = "Fred"
            p.save(flush:true)
            session.clear()

        when:
            p = PersonEvent.get(p.id)
            p.delete(flush:true)

        then:
            assert PersonEvent.STORE['deleted'] == true
    }

    void "Test before update event"() {
        given:
            session = setupEventsSession()
            PersonEvent.resetStore()

            def p = new PersonEvent()

            p.name = "Fred"
            p.save(flush:true)
            session.clear()

        when:
            p = PersonEvent.get(p.id)

        then:
            "Fred" == p.name
            0 == PersonEvent.STORE['updated']

        when:
            p.name = "Bob"
            p.save(flush:true)
            session.clear()
            p = PersonEvent.get(p.id)
        then:
            "Bob" == p.name
            1 == PersonEvent.STORE['updated']
    }

    void "Test before insert event"() {
        given:
            session = setupEventsSession()
            PersonEvent.resetStore()
            def p = new PersonEvent()

            p.name = "Fred"
            p.save(flush:true)
            session.clear()

        when:
            p = PersonEvent.get(p.id)

        then:
            "Fred" == p.name
            0 == PersonEvent.STORE['updated']
            1 == PersonEvent.STORE['inserted']

        when:
            p.name = "Bob"
            p.save(flush:true)
            session.clear()
            p = PersonEvent.get(p.id)

        then:
            "Bob" == p.name
            1 == PersonEvent.STORE['updated']
            1 == PersonEvent.STORE['inserted']
    }
}

class PersonEvent implements Serializable {
    Long id
    Long version
    String name
    Date dateCreated
    Date lastUpdated

    static STORE = [updated:0, inserted:0]

    static void resetStore() { STORE = [updated:0, inserted:0] }

    def beforeDelete() {
        STORE["deleted"] = true
    }

    def beforeUpdate() {
        STORE["updated"]++
    }

    def beforeInsert() {
        STORE["inserted"]++
    }
}

class ModifyPerson implements Serializable {
    Long id
    Long version

    String name

    def beforeInsert() {
        name = "Fred"
    }
}
