package org.grails.datastore.gorm

import org.junit.Test
import org.springframework.datastore.redis.RedisDatastore
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext

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
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)

    new GormEnhancer(redis).enhance()

    def con = redis.connect(null)
    con.getNativeInterface().flushdb()

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
}

class TestEntity {
  Long id
  String name
}
