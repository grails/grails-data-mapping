package org.grails.datastore.gorm

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.springframework.context.support.GenericApplicationContext
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.datastore.mapping.simpledb.SimpleDBDatastore
import org.grails.datastore.gorm.simpledb.SimpleDBGormEnhancer

/**
 * @author graemerocher
 * @author Roman Stepanenko
 */
class Setup {

    static simpleDB
    static session

    static destroy() {
        session.nativeInterface.dropDatabase()
    }

    static Session setup(classes) {
        simpleDB = new SimpleDBDatastore()
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        simpleDB.applicationContext = ctx
        simpleDB.afterPropertiesSet()

        for (cls in classes) {
            simpleDB.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = simpleDB.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        simpleDB.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def enhancer = new SimpleDBGormEnhancer(simpleDB, new DatastoreTransactionManager(datastore: simpleDB))
        enhancer.enhance()

        simpleDB.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        simpleDB.applicationContext.addApplicationListener new DomainEventListener(simpleDB)
        simpleDB.applicationContext.addApplicationListener new AutoTimestampEventListener(simpleDB)

        session = simpleDB.connect()

        return session
    }
}
