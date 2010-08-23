package org.grails.datastore.gorm

import org.junit.After
import org.springframework.datastore.redis.RedisDatastore
import org.junit.Before
import org.springframework.datastore.core.Session
import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 18, 2010
 * Time: 1:05:37 PM
 * To change this template use File | Settings | File Templates.
 */
class RangeQueryTests {

  Session con
  @Before
  void setupRedis() {
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)

    new GormEnhancer(redis).enhance()

    con = redis.connect(null)
    con.getNativeInterface().flushdb()
  }

  @After
  void disconnect() {
    con.disconnect()
  }


  @Test
  void testBetween() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }


    def results = TestEntity.findAllByAgeBetween(38, 40)

    assert 3 == results.size()

    results = TestEntity.findAllByAgeBetween(38, 40)

    assert 3 == results.size()

    assert results.find{ it.name == "Bob" }
    assert results.find{ it.name == "Fred" }
    assert results.find{ it.name == "Barney" }

    results = TestEntity.findAllByAgeBetweenOrName(38, 40, "Ernie")
    assert 4 == results.size()
  }


  @Test
  void testGreaterThanEqualsAndLessThanEquals() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }


    def results = TestEntity.findAllByAgeGreaterThanEquals(38)
    assert 3 == results.size()
    assert results.find { it.age == 38 }
    assert results.find { it.age == 39 }
    assert results.find { it.age == 40 }



    results = TestEntity.findAllByAgeLessThanEquals(38)
    results.each { println it.age }
    assert 4 == results.size()
    assert results.find { it.age == 38 }
    assert results.find { it.age == 37 }
    assert results.find { it.age == 36 }
    assert results.find { it.age == 35 }
  }
}
