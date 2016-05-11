package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple

/**
 * Created by graemerocher on 06/05/16.
 */
class DeleteSpec extends RxGormSpec {

    void "test delete entity"() {
        when:"creating an existing entity"
        Simple s = new Simple(name: "Fred").save().toBlocking().first()

        then:"The entity is created"
        Simple.count().toBlocking().first() == 1

        when:"The entity is deleted"
        def result = s.delete().toBlocking().first()

        then:"It is gone"
        result
        Simple.count().toBlocking().first() == 0
    }
    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
