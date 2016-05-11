package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple

/**
 * Created by graemerocher on 09/05/16.
 */
class SaveAllSpec extends RxGormSpec {

    void "test save all with new entities"() {
        when:"saveAll is called with new instances"
        def s1 = new Simple(name: "Bob")
        def s2 = new Simple(name: "Fred")
        def identifiers = Simple.saveAll(s1, s2).toBlocking().first()


        then:"The entities are saved"
        s1.id
        s2.id
        identifiers.size() == 2
        Simple.count().toBlocking().first() == 2

    }


    void "test save all with one new entity and one existing entity"() {
        when:"saveAll is called with new instances"
        def s1 = new Simple(name: "Bob").save().toBlocking().first()
        def s2 = new Simple(name: "Fred")
        def identifiers = Simple.saveAll(s1, s2).toBlocking().first()

        then:"The entities are saved"
        identifiers.size() == 2
        Simple.count().toBlocking().first() == 2

        when:"saveAll is called with one updated instance"
        def s3 = new Simple(name: "Joe")
        s1.name = "Jeff"
        identifiers = Simple.saveAll(s1, s3).toBlocking().first()

        then:"The entities are saved"
        identifiers.size() == 2
        Simple.count().toBlocking().first() == 3
        Simple.get(s1.id).toBlocking().first().name == 'Jeff'

    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
