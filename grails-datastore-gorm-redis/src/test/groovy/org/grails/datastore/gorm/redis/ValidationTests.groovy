package org.grails.datastore.gorm.redis

import org.junit.After
import org.junit.Before
import org.springframework.datastore.core.Session
import static org.grails.datastore.gorm.redis.RedisSetup.setup
import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity
import org.springframework.datastore.redis.RedisDatastore
import org.springframework.validation.Validator
import org.springframework.validation.Errors
import org.springframework.util.StringUtils

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 26, 2010
 * Time: 11:33:36 AM
 * To change this template use File | Settings | File Templates.
 */
class ValidationTests extends grails.gorm.tests.ValidationTests{

  Session con
  @Before
  void setupRedis() {
    def redis = new RedisDatastore()
    def context = redis.mappingContext
    def entity = context.addPersistentEntity(TestEntity)
    context.addPersistentEntity(ChildEntity)

    context.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if(!StringUtils.hasText(o.name)) {
                  errors.rejectValue("name", "name.is.blank")
                }
            }
    ] as Validator)

    new RedisGormEnhancer(redis).enhance()

    con = redis.connect()
    con.getNativeInterface().flushdb()

  }
  

  @After
  void disconnect() {
    con.disconnect()
  }
}
