package grails.gorm.tests


/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 4:56:21 PM
 * To change this template use File | Settings | File Templates.
 */
class CrudOperationsSpec extends GormDatastoreSpec{

  void "Test get returns null of non-existent entity"() {
    given:
      def t
    when:
      t = TestEntity.get(1)
    then:
      t == null
  }

  void "Test basic CRUD operations"() {
    given:

      def t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

    when:
      def results = TestEntity.list()
      t = TestEntity.get(t.id)


    then:
      t != null
      t.id != null
      "Bob" == t.name
      1 == results.size()
      "Bob" == results[0].name


  }


  void "Test save method that takes a map"() {

    given:
      def t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save(param:"one")
    when:
      t = TestEntity.get(t.id)
    then:
      t.id != null

  }

}
