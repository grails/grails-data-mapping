package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.events.AutoTimestampEventListener

class Setup {

    static session
    static datastore
    static trans

    static destroy() {
        trans.rollback()
        session.nativeInterface.shutdown()
        //new File(datastore.storeDir).deleteDir()
    }

    static Session setup(classes) {

        datastore = new Neo4jDatastore()

        def ctx = new GenericApplicationContext()
        ctx.refresh()
        datastore.applicationContext = ctx
        datastore.afterPropertiesSet()

        for (cls in classes) {
            datastore.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = datastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        datastore.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def enhancer = new Neo4jGormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
        enhancer.enhance()

        datastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        session = datastore.connect()
        trans = session.beginTransaction()
        session
    }

}
