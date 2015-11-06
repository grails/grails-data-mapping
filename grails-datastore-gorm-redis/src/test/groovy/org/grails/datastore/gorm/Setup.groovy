package org.grails.datastore.gorm

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.redis.*
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.redis.RedisDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
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
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        redis = new RedisDatastore(new KeyValueMappingContext(""), [pooled:"false"], ctx)
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

        def enhancer = new GormEnhancer(redis, new DatastoreTransactionManager(datastore: redis))

        redis.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        redis.applicationContext.addApplicationListener new DomainEventListener(redis)
        redis.applicationContext.addApplicationListener new AutoTimestampEventListener(redis)

        def con = redis.connect()
        con.getNativeInterface().flushdb()
        return con
    }
}
