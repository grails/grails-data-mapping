package grails.gorm.tests

class NullValueEqualSpec extends GormDatastoreSpec {

  void "test not in list returns the correct results"() {
    when:
    new TestEntity(name:"Fred").save()
    new TestEntity(name:"Bob", age: 11).save()
    new TestEntity(name:"Jack").save(flush:true)

    then:
    TestEntity.countByAge(null) == 2
    TestEntity.countByAge(11) == 1
    TestEntity.countByAgeNotEqual(11) == 2
    TestEntity.countByAgeNotEqual(null) == 1
  }
}
