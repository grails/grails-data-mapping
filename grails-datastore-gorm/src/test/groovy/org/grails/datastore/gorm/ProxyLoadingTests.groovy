package org.grails.datastore.gorm

import org.junit.After
import org.springframework.datastore.redis.RedisDatastore
import org.junit.Before
import org.springframework.datastore.core.Session
import org.junit.Test
import org.springframework.datastore.proxy.EntityProxy

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 11:20:18 AM
 * To change this template use File | Settings | File Templates.
 */
class ProxyLoadingTests {

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
  void testProxy() {

    def t = new TestEntity(name:"Bob", age: 45, child:new ChildEntity(name:"Test Child")).save()

    def proxy = TestEntity.load(t.id)

    assert proxy
    assert t.id == proxy.id

    assert "Bob" == proxy.name
  }

  @Test
  void testProxyWithQueryByAssociation() {
    def child = new ChildEntity(name: "Test Child")
    def t = new TestEntity(name:"Bob", age: 45, child:child).save()


    def proxy = ChildEntity.load(child.id)

    t = TestEntity.findByChild(proxy)

    assert t

  }
}
