package grails.gorm.rx.mongodb

import rx.Observable
import rx.Single

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

    void "Test findOrCreateWhere method"() {
        when:"findOrCreateWhere is used"
        def s = Simple.findOrCreateWhere(name:"Fred")
        def simple = s.toBlocking().first()
        then:"An instance is created"
        simple.name == "Fred"
        !simple.id
    }

    void "Test findOrSaveWhere method"() {
        when:"findOrSaveWhere is used"
        def s = Simple.findOrSaveWhere(name:"Fred")
        def simple = s.toBlocking().first()
        then:"An instance is created"
        simple.name == "Fred"
        simple.id
    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
