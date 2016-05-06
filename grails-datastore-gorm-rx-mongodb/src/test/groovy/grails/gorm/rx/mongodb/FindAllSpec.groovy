package grails.gorm.rx.mongodb

import rx.Observable

/**
 * Created by graemerocher on 06/05/16.
 */
class FindAllSpec extends RxGormSpec {

    void "Test findWhere query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        when:"A where query is created"
        Observable<Simple> result = Simple.findWhere(name:"Fred")

        then:"The result is an result"
        result.toBlocking().first().name == 'Fred'
    }

    void "Test findAllWhere query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        when:"A where query is created"
        Observable<Simple> result = Simple.findWhere(name:"Fred")

        then:"The result is an result"
        result.toBlocking().first().name == 'Fred'
    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
