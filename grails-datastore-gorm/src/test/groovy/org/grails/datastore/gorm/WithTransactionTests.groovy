package org.grails.datastore.gorm

import org.junit.After
import org.springframework.datastore.redis.RedisDatastore
import org.junit.Before
import org.springframework.datastore.core.Session
import org.junit.Test
import org.springframework.datastore.transactions.DatastoreTransactionManager

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 20, 2010
 * Time: 12:13:14 PM
 * To change this template use File | Settings | File Templates.
 */
class WithTransactionTests extends GroovyTestCase {

  Session con
  @Before
  void setupRedis() {
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)

    def manager = new DatastoreTransactionManager(redis)
    new GormEnhancer(redis, manager).enhance()

    con = redis.connect(null)
    con.getNativeInterface().flushdb()
  }

  @After
  void disconnect() {
    con.disconnect()
  }

  @Test
  void testWithTransaction() {

    // Mark as failing for the moment as we cannot properly create indices
    // within a Redis transaction at the moment. We need to hold of
    // creation on indices until the transaction is committed
    if(notYetImplemented()) return

    TestEntity.withTransaction {
      new TestEntity(name:"Bob", age:50, child:new ChildEntity(name:"Bob Child")).save()
      new TestEntity(name:"Fred", age:45, child:new ChildEntity(name:"Fred Child")).save()
    }
    
    assert 2 == TestEntity.count()

  }
}
