package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple

/**
 * Created by graemerocher on 06/05/16.
 */
class WhereSpec extends RxGormSpec {

    void "Test simple where query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        when:"A where query is created"
        def query = Simple.where {
            name == 'Fred'
        }

        then:"The result is an observable"
        query.find().toBlocking().first().name == 'Fred'
        query.findAll().toBlocking().first().name == 'Fred'
        query.list().toBlocking().first().size() == 1
        query.count().toBlocking().first() == 1

    }

    void "Test find and findAll query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        when:"A where query is created"
        def observable1 = Simple.findAll {
            name == 'Fred'
        }
        def observable2 = Simple.find {
            name == 'Fred'
        }

        then:"The result is an observable"
        observable1.toBlocking().first().name == 'Fred'
        observable2.toBlocking().first().name == 'Fred'

    }

    void "Test deleteAll where query"() {
        when:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        then:"The instance exists"
        Simple.findAll().toList().toBlocking().first()

        when:"A where query is created"
        def result = Simple.where {
            name == 'Fred'
        }.deleteAll().toBlocking().first()

        then:"The result is an observable"
        result == 1
        !Simple.list().toBlocking().first()
    }

    void "Test updateAll where query"() {
        when:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        then:"The instance exists"
        Simple.findAll().toList().toBlocking().first()

        when:"A where query is created"
        def result = Simple.where {
            name == 'Fred'
        }.updateAll(name:"Bob").toBlocking().first()

        then:"The result is an observable"
        result == 1
        !Simple.findByName("Fred").toList().toBlocking().first()
        Simple.findByName("Bob").toBlocking().first()
    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
