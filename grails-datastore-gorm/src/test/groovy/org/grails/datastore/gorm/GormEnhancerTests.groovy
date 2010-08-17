package org.grails.datastore.gorm

import org.junit.Test
import org.springframework.datastore.redis.RedisDatastore
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext
import org.springframework.datastore.core.Session
import org.junit.Before
import org.junit.After

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 11, 2010
 * Time: 4:47:41 PM
 * To change this template use File | Settings | File Templates.
 */
class GormEnhancerTests {

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
  void testCRUD() {
    def t = TestEntity.get(1)

    assert t == null

    t = new TestEntity(name:"Bob")
    t.save()

    assert t.id

    def results = TestEntity.list()

    assert 1 == results.size()
    assert "Bob" == results[0].name

    t = TestEntity.get(t.id)

    assert t
    assert "Bob" == t.name
  }

  @Test
  void testSaveWithMap() {
    def t = TestEntity.get(1)

    assert t == null

    t = new TestEntity(name:"Bob")
    t.save(param:"one")

    assert t.id

  }


  @Test
  void testDynamicFinder() {

    Session con = setupRedis()


    def t = new TestEntity(name:"Bob")
    t.save()

    t = new TestEntity(name:"Fred")
    t.save()

    def results = TestEntity.list()

    assert 2 == results.size()

    def bob = TestEntity.findByName("Bob")

    assert bob
    assert "Bob" == TestEntity.findByName("Bob").name 
  }

  @Test
  void testDisjunction() {
    def age = 40
    ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++).save() }

    assert 3 == TestEntity.list().size()

    def results = TestEntity.findAllByNameOrAge("Barney", 40)

    assert 2 == results.size()

    def barney = results.find { it.name == "Barney" }
    assert barney
    assert 42 == barney.age
    def bob = results.find { it.age == 40 }
    assert bob
    assert "Bob" == bob.name
    
  }

  @Test
  void testFinderWithPagination() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }

    assert 4 == TestEntity.list().size()
    assert 4 == TestEntity.count()

    assert 2 == TestEntity.findAllByNameOrAge("Barney", 40).size()
    assert 1 == TestEntity.findAllByNameOrAge("Barney", 40, [max:1]).size()

  }

  @Test
  void testCountByQuery() {

    def age = 40
    ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++).save() }

    assert 3 == TestEntity.list().size()

    assert 2 == TestEntity.countByNameOrAge("Barney", 40)
    assert 1 == TestEntity.countByNameAndAge("Bob", 40)
  }

  @Test
  void testConjunction() {
    def age = 40
    ["Bob", "Fred", "Barney"].each { new TestEntity(name:it, age: age++).save() }

    assert 3 == TestEntity.list().size()

    assert TestEntity.findByNameAndAge("Bob", 40)
    assert !TestEntity.findByNameAndAge("Bob", 41)
  }

  @Test
  void testCount() {
    def t = new TestEntity(name:"Bob")
    t.save()

    assert 1 == TestEntity.count()

    t = new TestEntity(name:"Fred")
    t.save()

    assert 2 == TestEntity.count()

  }
}

class TestEntity {
  Long id
  String name
  Integer age

  static mapping = {
    name index:true
    age index:true
  }
}
