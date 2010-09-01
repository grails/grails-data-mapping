package grails.gorm.tests

import org.junit.Ignore

/**
 * Abstract base test for testing transactions. Subclasses should do the necessary setup to configure GORM
 */
class WithTransactionSpec extends GormDatastoreSpec{

  void "Test save() with transaction"() {
    given:
      TestEntity.withTransaction {
        new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
        new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
      }

    when:
      int count = TestEntity.count()
      def results = TestEntity.list(sort:"name")
    then:
      2 == count
      "Bob" == results[0].name
      "Fred" == results[1].name

  }


  void "Test rollback transaction"() {
    given:
      TestEntity.withTransaction { status ->
        new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
        status.setRollbackOnly()
        new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
      }

    when:
      int count = TestEntity.count()
      def results = TestEntity.list(sort:"name")

    then:
      count == 0
      results.size() == 0
  }

  void "Test rollback transaction with Exception"() {
    given:

      try {
        TestEntity.withTransaction { status ->
          new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
          throw new RuntimeException("bad")
          new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
        }
      } catch (e) {
        // ignore
      }

    when:
      int count = TestEntity.count()
      def results = TestEntity.list(sort:"name")

    then:
      count == 0
      results.size() == 0
  }
}
