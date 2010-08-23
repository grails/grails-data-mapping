package org.grails.datastore.gorm.redis

import org.junit.After
import org.junit.Before
import org.springframework.datastore.core.Session
import static org.grails.datastore.gorm.redis.RedisSetup.setup
import org.junit.Test
import grails.gorm.tests.TestEntity
import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 1:31:03 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisSpecificMethodsTests {
  Session con
  @Before
  void setupRedis() {
    con = setup()
  }

  @After
  void disconnect() {
    con.disconnect()
  }


  @Test
  void testGetRandom() {
    def age = 40
    def names = ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"]
    names.each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }


    def t = TestEntity.random()

    assert t != null
    assert names.find { it == t.name }
  }

  @Test
  void testPop() {
    def age = 40
    def names = ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"]
    names.each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }

    assert 6 == TestEntity.count()
    def t = TestEntity.pop()

    assert t != null
    assert names.find { it == t.name }

    assert 5 == TestEntity.count()
  }

}
