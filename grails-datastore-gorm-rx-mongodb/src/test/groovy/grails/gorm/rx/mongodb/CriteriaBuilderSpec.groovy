package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple
import rx.Observable

/**
 * Created by graemerocher on 06/05/16.
 */
class CriteriaBuilderSpec extends RxGormSpec {

    void "Test simple criteria query"() {
        given:"An existing instance"
        new Simple(name: "Fred").save().toBlocking().first()

        when:"A where query is created"
        Observable result = Simple.withCriteria {
            eq 'name', 'Fred'
        }

        then:"The result is an observable"
        result.toBlocking().first().name == 'Fred'

    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
