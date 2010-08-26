package grails.gorm.tests

import org.junit.Test

/**
 * Abstract base test for order by queries. Subclasses should do the necessary setup to configure GORM
 */
abstract class OrderByTests extends AbstractGormTests{


  @Test
  void testOrderBy() {
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
    }


    def results = TestEntity.list(sort:"age")

    assert 40 == results[0].age
    assert 41 == results[1].age
    assert 42 == results[2].age

    results = TestEntity.list(sort:"age", order:"desc")


    assert 45 == results[0].age
    assert 44 == results[1].age
    assert 43 == results[2].age

  }

  @Test
  void testOrderByInDynamicFinder() {
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
    }


    def results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort:"age"])

    assert 40 == results[0].age
    assert 41 == results[1].age
    assert 42 == results[2].age

    results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort:"age", order:"desc"])


    assert 45 == results[0].age
    assert 44 == results[1].age
    assert 43 == results[2].age
  }
}
