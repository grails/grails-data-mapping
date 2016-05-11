package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple

/**
 * Created by graemerocher on 09/05/16.
 */
class DeleteAllSpec extends RxGormSpec {

    void "Test deleteAll method"() {
        when:"2 objects are saved"
        def o1 = new Simple(name: "Bob").save().toBlocking().first()
        def o2 = new Simple(name: "Fred").save().toBlocking().first()

        then:"The count is correct"
        Simple.count().toBlocking().first() == 2

        when:"They are batch deleted"
        def deleteCount = Simple.deleteAll(o1, o2).toBlocking().first()

        then:"The count is correct"
        deleteCount == 2
        Simple.count().toBlocking().first() == 0

    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
