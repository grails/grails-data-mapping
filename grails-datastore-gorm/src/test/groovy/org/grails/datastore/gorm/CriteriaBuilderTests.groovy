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
 * Time: 11:14:13 AM
 * To change this template use File | Settings | File Templates.
 */
class CriteriaBuilderTests {


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
  void testListQuery() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }


    def criteria = TestEntity.createCriteria()

    def results = criteria.list {
       like('name', 'B%')
    }

    assert 2 == results.size()

    results = criteria.list {
       like('name', 'B%')
       max 1
    }

    assert 1 == results.size()
  }

  @Test
  void testCount() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }


    def criteria = TestEntity.createCriteria()

    def result = criteria.count {
       like('name', 'B%')
    }

    assert 2 == result
  }

  @Test
  void testSingleResult() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }


    def criteria = TestEntity.createCriteria()

    def result = criteria.get {
       eq('name', 'Bob')
    }

    assert result != null
    assert "Bob" == result.name

  }
}
