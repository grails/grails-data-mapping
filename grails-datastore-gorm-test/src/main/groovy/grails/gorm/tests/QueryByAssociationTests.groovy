package grails.gorm.tests

import org.junit.Test

/**
 * Abstract base test for query associations. Subclasses should do the necessary setup to configure GORM
 */
abstract class QueryByAssociationTests {


  @Test
  void testQueryByAssociation() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def child = ChildEntity.findByName("Barney Child")

    assert child

    def t = TestEntity.findByChild(child)

    assert t
    assert "Barney" == t.name
  }
}
