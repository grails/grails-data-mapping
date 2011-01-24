package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: Erawat
 * Date: 31-Oct-2010
 * Time: 10:54:39
 * To change this template use File | Settings | File Templates.
 */
class GormEnhancerSpec extends GormDatastoreSpec {

  def cleanup() {
    def nativeSession  = session.nativeInterface
    def wp = nativeSession.getWorkspace();
    def qm = wp.getQueryManager();

    def q = qm.createQuery("//Child", javax.jcr.query.Query.XPATH);
    def qr = q.execute()
    def itr = qr.getNodes();
    itr.each { it.remove() }

    q = qm.createQuery("//TestEntity", javax.jcr.query.Query.XPATH);
    qr = q.execute()
    itr = qr.getNodes();
    itr.each { it.remove() }
    nativeSession.save()

  }

  void "Test basic CRUD operations"() {
    given:
      def t

    when:
      //JCR plugin supports only get() by using value UUID
      t = TestEntity.get("19503d32-fb94-4f85-b50c-681e6f1a02f2");
    then:
      t == null

    when:
      t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

    then:
      t.id != null

    when:
      def results = TestEntity.list()

    then:
      1 == results.size()
      "Bob" == results[0].name

    when:
      t = TestEntity.get(t.id)

    then:
      t != null
      "Bob" == t.name
  }



  void "Test simple dynamic finder"() {

    given:
      def t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

      t = new TestEntity(name:"Fred", child:new ChildEntity(name:"Child"))
      t.save()

    when:
      def results = TestEntity.list()
      def bob = TestEntity.findByName("Bob")

    then:
      2 == results.size()
      bob != null
      "Bob" == bob.name
  }


  void "Test dynamic finder with disjunction"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    when:
      def results = TestEntity.findAllByNameOrAge("Barney", 40)
      def barney = results.find { it.name == "Barney" }
      def bob = results.find { it.age == 40 }
    then:
      3 == TestEntity.count()
      2 == results.size()
      barney != null
      42 == barney.age
      bob != null
      "Bob" == bob.name

  }

  void "Test getAll() method"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def results = TestEntity.getAll('19503d32-fb94-4f85-b50c-681e6f1a02f2','19503d32-fb94-4f85-b50c-681e6f1a02f3')
    then:
      2 == results.size()

  }


  void "Test ident() method"() {
    given:
    def t

    when:
      t= new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

    then:
      t.id != null
      t.id == t.ident()
  }

  void "Test dynamic finder with pagination parameters"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def total = TestEntity.count()
    then:
      4 == total

      2 == TestEntity.findAllByNameOrAge("Barney", 40).size()
      1 == TestEntity.findAllByNameOrAge("Barney", 40, [max:1]).size()
  }


  void "Test in list query"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def total = TestEntity.count()
    then:
      4 == total
      2 == TestEntity.findAllByNameInList(["Fred", "Frank"]).size()
      1 == TestEntity.findAllByNameInList(["Joe", "Frank"]).size()
      0 == TestEntity.findAllByNameInList(["Jeff", "Jack"]).size()
      2 == TestEntity.findAllByNameInListOrName(["Joe", "Frank"], "Bob").size()
  }

  void "Test like query"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def results = TestEntity.findAllByNameLike("Fr%")

    then:
      2 == results.size()
      results.find { it.name == "Fred" } != null
      results.find { it.name == "Frank" } != null
  }


  void "Test count by query"() {

    given:
      def age = 40
      ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def total = TestEntity.count()
    then:
      3 == total
      3 == TestEntity.list().size()
      2 == TestEntity.countByNameOrAge("Barney", 40)
      1 == TestEntity.countByNameAndAge("Bob", 40)
  }

  void "Test dynamic finder with conjunction"() {
    given:
      def age = 40
      ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    when:
      def total = TestEntity.count()
    then:
      3 == total
      3 == TestEntity.list().size()

      TestEntity.findByNameAndAge("Bob", 40)
      !TestEntity.findByNameAndAge("Bob", 41)
  }


  void "Test count() method"() {
    given:
      def t

    when:
      t= new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
      t.save()

    then:
      1 == TestEntity.count()

    when:
      t = new TestEntity(name:"Fred", child:new ChildEntity(name:"Child"))
      t.save()

    then:
      2 == TestEntity.count()
  }
}
