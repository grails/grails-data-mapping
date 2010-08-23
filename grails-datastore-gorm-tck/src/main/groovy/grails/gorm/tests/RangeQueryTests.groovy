package grails.gorm.tests
import org.junit.Test

/**
 * Abstract base test for querying ranges. Subclasses should do the necessary setup to configure GORM
 */
abstract class RangeQueryTests {


  @Test
  void testBetween() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }


    def results = TestEntity.findAllByAgeBetween(38, 40)

    assert 3 == results.size()

    results = TestEntity.findAllByAgeBetween(38, 40)

    assert 3 == results.size()

    assert results.find{ it.name == "Bob" }
    assert results.find{ it.name == "Fred" }
    assert results.find{ it.name == "Barney" }

    results = TestEntity.findAllByAgeBetweenOrName(38, 40, "Ernie")
    assert 4 == results.size()
  }


  @Test
  void testGreaterThanEqualsAndLessThanEquals() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }


    def results = TestEntity.findAllByAgeGreaterThanEquals(38)
    assert 3 == results.size()
    assert results.find { it.age == 38 }
    assert results.find { it.age == 39 }
    assert results.find { it.age == 40 }



    results = TestEntity.findAllByAgeLessThanEquals(38)
    results.each { println it.age }
    assert 4 == results.size()
    assert results.find { it.age == 38 }
    assert results.find { it.age == 37 }
    assert results.find { it.age == 36 }
    assert results.find { it.age == 35 }
  }
}
