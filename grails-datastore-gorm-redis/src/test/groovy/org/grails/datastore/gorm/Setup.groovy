package org.grails.datastore.gorm

import org.grails.datastore.gorm.redis.*
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.redis.RedisDatastore
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * @author graemerocher
 */
class Setup {

    static redis

    static destroy() {
        redis?.destroy()
    }

    static Session setup(classes) {
        redis = new RedisDatastore(new KeyValueMappingContext(""), [pooled:"false"])
        for (cls in classes) {
            redis.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = redis.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        redis.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def enhancer = new RedisGormEnhancer(redis, new DatastoreTransactionManager(datastore: redis))
        enhancer.enhance()

        redis.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        def con = redis.connect()
        con.getNativeInterface().flushdb()
        return con
    }
}
