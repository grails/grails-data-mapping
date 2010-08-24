package grails.gorm.tests

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 4:56:21 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class CrudOperationsTests {

  @Test
  void testCRUD() {
    def t = TestEntity.get(1)

    assert t == null

    t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
    t.save()

    assert t.id

    def results = TestEntity.list()

    assert 1 == results.size()
    assert "Bob" == results[0].name

    t = TestEntity.get(t.id)

    assert t
    assert "Bob" == t.name
  }

  @Test
  void testSaveWithMap() {
    def t = TestEntity.get(1)

    assert t == null

    t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
    t.save(param:"one")

    assert t.id

  }

}
