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
    ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }


    
  }
}
