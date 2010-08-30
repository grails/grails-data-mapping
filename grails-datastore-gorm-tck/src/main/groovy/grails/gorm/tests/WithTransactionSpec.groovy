package grails.gorm.tests

import org.junit.Ignore

/**
 * Abstract base test for testing transactions. Subclasses should do the necessary setup to configure GORM
 */
class WithTransactionSpec extends GormDatastoreSpec{

  @Ignore
  void "Test save() with transaction"() {

    TestEntity.withTransaction {
      new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
      new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
    }
    
    assert 2 == TestEntity.count()

  }
}
