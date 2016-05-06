package grails.gorm.rx.mongodb

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
        query.toList().toBlocking().first().size() == 1
        query.total().toBlocking().first() == 1

    }

    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
