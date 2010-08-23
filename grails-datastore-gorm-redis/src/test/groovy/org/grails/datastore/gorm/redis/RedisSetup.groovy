package org.grails.datastore.gorm.redis


import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity
import org.springframework.datastore.core.Session
import org.springframework.datastore.redis.RedisDatastore

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 12:31:02 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisSetup {
  static Session setup() {
    def redis = new RedisDatastore()
    redis.mappingContext.addPersistentEntity(TestEntity)
    redis.mappingContext.addPersistentEntity(ChildEntity)

    new RedisGormEnhancer(redis).enhance()

    def con = redis.connect()
    con.getNativeInterface().flushdb()
    return con
  }

}
