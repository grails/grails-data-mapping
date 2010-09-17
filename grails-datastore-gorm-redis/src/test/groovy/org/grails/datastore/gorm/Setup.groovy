package org.grails.datastore.gorm

import org.grails.datastore.gorm.redis.*
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.redis.RedisDatastore
import org.springframework.validation.Validator
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 12:31:02 PM
 * To change this template use File | Settings | File Templates.
 */
class Setup {
  static Session setup(classes) {
    def redis = new RedisDatastore()
    for(cls in classes) {
      redis.mappingContext.addPersistentEntity(cls)
    }

    PersistentEntity entity = redis.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}    

    redis.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if(!StringUtils.hasText(o.name)) {
                  errors.rejectValue("name", "name.is.blank")
                }
            }
    ] as Validator)
    
    new RedisGormEnhancer(redis, new DatastoreTransactionManager(datastore:redis)).enhance()

    def con = redis.connect()
    con.getNativeInterface().flushdb()
    return con
  }

}
