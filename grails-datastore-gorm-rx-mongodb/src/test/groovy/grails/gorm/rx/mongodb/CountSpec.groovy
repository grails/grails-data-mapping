package grails.gorm.rx.mongodb

/**
 * Created by graemerocher on 06/05/16.
 */
class CountSpec extends RxGormSpec {

    void "Test count method with no data"() {
        expect:
            Simple.count().toBlocking().first() == 0
    }

    void "Test count method with existing objects"() {
        when:"There are objects"
        new Simple(name: "Fred").save().toBlocking().first()
        new Simple(name: "Bob").save().toBlocking().first()

        then:"The count is correct"
        Simple.count().toBlocking().first() == 2
    }
    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
