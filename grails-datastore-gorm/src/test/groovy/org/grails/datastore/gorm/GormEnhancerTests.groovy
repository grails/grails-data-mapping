package org.grails.datastore.gorm

import org.junit.Test
import org.springframework.datastore.redis.RedisDatastore
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext
import org.springframework.datastore.core.Session

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 11, 2010
 * Time: 4:47:41 PM
 * To change this template use File | Settings | File Templates.
 */
class GormEnhancerTests {

  @Test
  void testCRUD() {
    Session con = setupRedis()

    def t = TestEntity.get(1)

    assert !t

    t = new TestEntity(name:"Bob")
    t.save()

    assert t.id

    def results = TestEntity.list()

    assert 1 == results.size()
    assert "Bob" == results[0].name

    t = TestEntity.get(t.id)

    assert t
    assert "Bob" == t.name


    con.disconnect()
  }

  private Session setupRedis() {
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)

    new GormEnhancer(redis).enhance()

    def con = redis.connect(null)
    con.getNativeInterface().flushdb()
    return con
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


    con.disconnect()
  }
}

class TestEntity {
  Long id
  String name

  static mapping = {
    name index:true
  }
}
