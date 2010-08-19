package org.grails.datastore.gorm

import org.junit.Test
import org.junit.After
import org.springframework.datastore.redis.RedisDatastore
import org.junit.Before
import org.springframework.datastore.core.Session

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 19, 2010
 * Time: 12:08:49 PM
 * To change this template use File | Settings | File Templates.
 */
class OrderByTests {

  Session con
  @Before
  void setupRedis() {
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)

    new GormEnhancer(redis).enhance()

    con = redis.connect()
    con.getNativeInterface().flushdb()
  }

  @After
  void disconnect() {
    con.disconnect()
  }


  @Test
  void testOrderBy() {
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++).save()
    }


    def results = TestEntity.list(sort:"age")

    assert 40 == results[0].age
    assert 41 == results[1].age
    assert 42 == results[2].age

    results = TestEntity.list(sort:"age", order:"desc")


    assert 45 == results[0].age
    assert 44 == results[1].age
    assert 43 == results[2].age

  }

  @Test
  void testOrderByInDynamicFinder() {
    def age = 40

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++).save()
    }


    def results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort:"age"])

    assert 40 == results[0].age
    assert 41 == results[1].age
    assert 42 == results[2].age

    results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort:"age", order:"desc"])


    assert 45 == results[0].age
    assert 44 == results[1].age
    assert 43 == results[2].age
  }
}
