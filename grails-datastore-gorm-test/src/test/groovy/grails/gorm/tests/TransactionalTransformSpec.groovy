package grails.gorm.tests

import grails.gorm.transactions.Transactional

/**
 * Created by graemerocher on 05/01/2017.
 */
class TransactionalTransformSpec extends GormDatastoreSpec {

    void "test transaction manager lookup with @Transactional and unassigned transaction manager"() {
        expect:
        new TestService().testMe()
    }

    @Override
    List getDomainClasses() {
        [Person]
    }


}
@Transactional
class TestService {

    boolean testMe() {
        return transactionStatus != null
    }
}