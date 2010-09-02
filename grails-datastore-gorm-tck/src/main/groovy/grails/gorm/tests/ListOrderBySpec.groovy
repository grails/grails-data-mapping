package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 2, 2010
 * Time: 12:31:10 PM
 * To change this template use File | Settings | File Templates.
 */
class ListOrderBySpec extends GormDatastoreSpec{

  void "Test listOrderBy property name method"() {
    given:
        def child = new ChildEntity(name: "Child")
        new TestEntity(age:30, name:"Bob", child:child).save()
        new TestEntity(age:55, name:"Fred", child:child).save()
        new TestEntity(age:17, name:"Jack", child:child).save()


    when:
      def results = TestEntity.listOrderByAge()

    then:
      results.size() == 3
      results[0].name == "Jack"
      results[1].name == "Bob"
      results[2].name == "Fred"
  }
}
