package org.grails.datastore.gorm.redis

import grails.gorm.tests.*

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 1:31:03 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisSpecificMethodsSpec extends GormDatastoreSpec {

  def "Test expire an entity"() {
    given:
      new TestEntity(name:"Bob", age:45, child:new ChildEntity(name:"Child")).save()
      session.flush()

    when:
      def te = TestEntity.random()

    then:
      te != null

    when:
      te.expire(5)
      session.clear()
      sleep(10000)
    then:
      TestEntity.get(te.id) == null

  }

  def "Test get random entity"() {
    given:
    def age = 40
    def names = ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"]
    names.each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }

    when:
    def t = TestEntity.random()

    then:
    t != null
    names.find { it == t.name }
  }

  def "Test pop random entity"() {
    given:
    def age = 40
    def names = ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"]
    names.each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }

    expect:
    6 == TestEntity.count()

    when:
    def t = TestEntity.pop()

    then:
    t != null
    names.find { it == t.name }
    5 == TestEntity.count()
  }

}
