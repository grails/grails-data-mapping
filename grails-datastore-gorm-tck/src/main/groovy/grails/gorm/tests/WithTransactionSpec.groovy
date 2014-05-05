package grails.gorm.tests

import groovy.transform.InheritConstructors
import org.springframework.transaction.TransactionDefinition

/**
 * Transaction tests.
 */
class WithTransactionSpec extends GormDatastoreSpec {

    void "Test save() with transaction"() {
        given:
            TestEntity.withTransaction {
                new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
                new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
            }

        when:
            int count = TestEntity.count()
//            def results = TestEntity.list(sort:"name") // TODO this fails but doesn't appear to be tx-related, so manually sorting
            def results = TestEntity.list().sort { it.name }

        then:
            2 == count
            "Bob" == results[0].name
            "Fred" == results[1].name
    }

    void "Test rollback transaction"() {
        given:
            TestEntity.withNewTransaction { status ->
                new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
                status.setRollbackOnly()
                new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
            }

        when:
            int count = TestEntity.count()
            def results = TestEntity.list()

        then:
            count == 0
            results.size() == 0
    }

    void 'Test specifying transaction properties for withTransaction'() {
        when:
        new TestEntity(name: 'One', age: 1).save()

        then:
        1 == TestEntity.count()

        when: 'the outer transaction rolls back and the inner transaction propagation is PROPAGATION_REQUIRES_NEW'
        TestEntity.withNewTransaction { status ->
            TestEntity.withTransaction([propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW]) {
                new TestEntity(name: 'Two', age: 2).save()
            }
            status.setRollbackOnly()
        }

        then: 'the inner transaction is committed'
        2 == TestEntity.count()

        when: 'the outer transaction rolls back and the inner transaction propagation is PROPAGATION_REQUIRED'
        TestEntity.withNewTransaction { status ->
            TestEntity.withTransaction([propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRED]) {
                new TestEntity(name: 'Three', age: 3).save()
            }
            status.setRollbackOnly()
        }

        then: 'the inner transaction is rolled back'
        2 == TestEntity.count()
    }

    void "Test rollback transaction with Runtime Exception"() {
        given:
            def ex
            try {
                TestEntity.withNewTransaction { status ->
                    new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
                    throw new RuntimeException("bad")
                }
            }
            catch (e) {
                ex = e
            }

        when:
            int count = TestEntity.count()
            def results = TestEntity.list()

        then:
            count == 0
            results.size() == 0
            ex instanceof RuntimeException
            ex.message == 'bad'
    }

    void "Test rollback transaction with Exception"() {
        given:
            def ex
            try {
                TestEntity.withNewTransaction { status ->
                    new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
                    throw new TestCheckedException("bad")
                }
            }
            catch (e) {
                ex = e
            }

        when:
            int count = TestEntity.count()
            def results = TestEntity.list()

        then:
            count == 1
            results.size() == 1
            ex instanceof TestCheckedException
            ex.message == 'bad'
    }
}

@InheritConstructors
class TestCheckedException extends Exception {}
