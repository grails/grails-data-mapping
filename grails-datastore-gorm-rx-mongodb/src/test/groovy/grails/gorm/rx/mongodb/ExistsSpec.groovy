package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 06/05/16.
 */
class ExistsSpec extends RxGormSpec {

    void "Test exists method"() {
        given:"An existing instance"
        Simple s = new Simple().save().toBlocking().first()

        expect:
        Simple.exists(s.id).toBlocking().first()
        !Simple.exists(new ObjectId()).toBlocking().first()
    }
    @Override
    List<Class> getDomainClasses() {
        [Simple]
    }
}
