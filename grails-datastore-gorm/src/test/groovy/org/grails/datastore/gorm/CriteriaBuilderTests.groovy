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
    redis.mappingContext.addPersistentEntity(ChildEntity)

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
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


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
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def criteria = TestEntity.createCriteria()

    def result = criteria.count {
       like('name', 'B%')
    }

    assert 2 == result
  }

  @Test
  void testSingleResult() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def criteria = TestEntity.createCriteria()

    def result = criteria.get {
       eq('name', 'Bob')
    }

    assert result != null
    assert "Bob" == result.name

  }

  @Test
  void testOrder() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def criteria = TestEntity.createCriteria()

    def results = criteria.list {
       like('name', 'B%')
       order "age"
    }

    assert "Bob" == results[0].name
    assert "Barney" == results[1].name

    results = criteria.list {
       like('name', 'B%')
       order "age", "desc"
    }

    assert "Barney" == results[0].name
    assert "Bob" == results[1].name

  }


  @Test
  void testMinProjection() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def criteria = TestEntity.createCriteria()

    def result = criteria.get {
      projections {
        min "age"
      }
    }

    assert 40 == result

    result = criteria.get {
      projections {
        max "age"
      }
    }

    assert 43 == result

    def results = criteria.list {
      projections {
        max "age"
        min "age"
      }
    }

    assert 2 == results.size()
    assert 43 == results[0]
    assert 40 == results[1]
    assert [43, 40] == results
  }

  @Test
  void testPropertyProjection() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def criteria = TestEntity.createCriteria()

    def results = criteria.list {
      projections {
        property "age"
      }
    }

    assert [40, 41, 42, 43] == results
  }

  @Test
  void testPropertyProjectionOnEntity() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

    assert 4 == ChildEntity.count()


    def criteria = TestEntity.createCriteria()

    def results = criteria.list {
      projections {
        property "child"
      }
    }

    assert results.find { it.name = "Bob Child"}
    assert results.find { it.name = "Fred Child"}
    assert results.find { it.name = "Barney Child"}
    assert results.find { it.name = "Frank Child"}

  }
}
