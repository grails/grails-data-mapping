package grails.gorm.tests

import org.junit.Test

/**
 * Abstract base test for testing transactions. Subclasses should do the necessary setup to configure GORM
 */
abstract class WithTransactionTests {

  @Test
  void testWithTransaction() {

    TestEntity.withTransaction {
      new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
      new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
    }
    
    assert 2 == TestEntity.count()

  }
}
