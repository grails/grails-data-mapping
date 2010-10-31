package grails.gorm.tests

/**
 * Override from GORM TCK to test String based Id
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class CrudOperationsSpec extends GormDatastoreSpec{

  void "Test get using a string-based key"() {
    given:

      def t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

    when:
      //No converter found capable of converting from 'org.codehaus.groovy.runtime.GStringImpl' to 'java.lang.String
      //t = TestEntity.get("${t.id}")
      //id is a String-based key
      t = TestEntity.get(t.id);

    then:
      t != null

  }
  void "Test get returns null of non-existent entity"() {
    given:
      def t
    when:
      //JCR plugin supports only get() by using value UUID
      t = TestEntity.get("19503d32-fb94-4f85-b50c-681e6f1a02f2");
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
