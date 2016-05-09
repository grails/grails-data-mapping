package grails.gorm.rx.mongodb

import rx.Observable
import rx.Single

/**
 * Created by graemerocher on 09/05/16.
 */
class FirstLastSpec extends RxGormSpec {

    void "Test first and last methods query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()
        new Simple(name: "Bob").save().toBlocking().first()

        when:"A where query is created"
        Single<Simple> first = Simple.first()
        Single<Simple> last = Simple.last()

        then:"The result is an result"
        first.toBlocking().value().name == 'Fred'
        last.toBlocking().value().name == 'Bob'
    }


    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}