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
 * Time: 5:25:04 PM
 * To change this template use File | Settings | File Templates.
 */
class QueryByAssociationTests {

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
  void testQueryByAssociation() {
    def age = 40
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }


    def child = ChildEntity.findByName("Barney Child")

    assert child

    def t = TestEntity.findByChild(child)

    assert t
    assert "Barney" == t.name
  }
}
