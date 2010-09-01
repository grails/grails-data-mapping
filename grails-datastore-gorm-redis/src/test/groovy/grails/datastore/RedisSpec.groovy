package grails.datastore

import grails.gorm.tests.*

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 12:46:51 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisSpec extends GormDatastoreSpec {

  def testRedisList() {
    given:
    def redis = new Redis(datastore:session.datastore)

    when:
    redis.flushall()
    def list = redis.list("my.list")
    list << 1 << 2 << 3

    then:
    3 == list.size()
    1 == list[0] as Integer
    2 == list[1] as Integer
    3 == list[2] as Integer
  }

  def testEntities() {
    given:
    def redis = new Redis(datastore:session.datastore)
    def age = 40                                                                                                 
    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
    }
    def results = TestEntity.list()

    expect:
    6 == results.size()

    when:
    def list = redis.list("my.list")
    results.each { list << it.id }
    def entities = redis.entities(TestEntity, "my.list")

    then:
    6 == entities.size()
    entities.find { it.name == "Fred"}
  }
}
