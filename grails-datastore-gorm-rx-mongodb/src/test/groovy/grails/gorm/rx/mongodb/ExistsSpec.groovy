package grails.gorm.rx.mongodb

/**
 * Created by graemerocher on 06/05/16.
 */
class ExistsSpec extends RxGormSpec {

    void "Test exists method"() {
        given:"An existing instance"
        Simple s = new Simple().save().toBlocking().first()

        expect:
        Simple.exists(s.id).toBlocking().first()
        !Simple.exists(10L).toBlocking().first()
    }
    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
