package grails.gorm.tests

/**
 * Abstract base test for order by queries. Subclasses should do the necessary setup to configure GORM
 */
class OrderBySpec extends GormDatastoreSpec {

  def cleanup() {
    def nativeSession = session.nativeInterface
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

  void "Test order by with list() method"() {
    given:
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name: it, age: age++, child: new ChildEntity(name: "$it Child")).save()
    }


    when:
    def results = TestEntity.list(sort: "age")

    then:
    40 == results[0].age
    41 == results[1].age
    42 == results[2].age

    when:
    results = TestEntity.list(sort: "age", order: "desc")


    then:
    45 == results[0].age
    44 == results[1].age
    43 == results[2].age

  }

  void "Test order by property name with dynamic finder"() {
    given:
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name: it, age: age++, child: new ChildEntity(name: "$it Child")).save()
    }


    when:
    def results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: "age"])

    then:
    40 == results[0].age
    41 == results[1].age
    42 == results[2].age

    when:
    results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: "age", order: "desc"])


    then:
    45 == results[0].age
    44 == results[1].age
    43 == results[2].age
  }
}
