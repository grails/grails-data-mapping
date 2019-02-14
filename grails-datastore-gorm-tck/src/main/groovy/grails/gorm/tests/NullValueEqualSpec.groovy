package grails.gorm.tests

class NullValueEqualSpec extends GormDatastoreSpec {

  void "test null value in equal and not equal"() {
    when:
    new TestEntity(name:"Fred").save(failOnError: true)
    new TestEntity(name:"Bob", age: 11).save(failOnError: true)
    new TestEntity(name:"Jack").save(flush:true, failOnError: true)

    then:
    TestEntity.countByAge(null) == 2
    TestEntity.countByAge(11) == 1
    TestEntity.countByAgeNotEqual(11) == 2
    TestEntity.countByAgeNotEqual(null) == 1
  }
}
