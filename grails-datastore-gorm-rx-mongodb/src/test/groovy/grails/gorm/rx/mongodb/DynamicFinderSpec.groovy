package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple
import rx.Observable

/**
 * Created by graemerocher on 06/05/16.
 */
class DynamicFinderSpec extends RxGormSpec {

    void "Test findBy finder returns an observable"() {
        when:"An entity is saved"
        Simple s = new Simple(name: "Fred")
        s.save().toBlocking().first()

        then:"It can be found with a finder"
        Simple.findByName("Fred").toBlocking().first().name == "Fred"
    }

    void "Test countBy finder returns an observable"() {
        when:"An entity is saved"
        Simple s = new Simple(name: "Fred")
        s.save().toBlocking().first()

        then:"It can be found with a finder"
        Simple.countByName("Fred").toBlocking().first() == 1
    }

    void "Test findAllBy finder returns an observable"() {
        when:"An entity is saved"
        Simple s = new Simple(name: "Fred")
        s.save().toBlocking().first()
        List results = Simple.findAllByName("Fred").toBlocking().iterator.toList()
        then:"It can be found with a finder"
        results.size() == 1
        results[0].name == "Fred"
    }

    void "Test findOrCreateBy finder"() {
        when:"A entity is created using"
        Observable<Simple> o = Simple.findOrCreateByName("Fred")


        then:"The entity is created"
        o.toBlocking().iterator.hasNext()
        o.toBlocking().first().name == 'Fred'
    }

    void "Test findOrSaveBy finder"() {
        when:"A entity is created using"
        Observable<Simple> o = Simple.findOrSaveByName("Fred")


        then:"The entity is created"
        o.toBlocking().iterator.hasNext()
        o.toBlocking().first().name == 'Fred'
        o.toBlocking().first().id
    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
