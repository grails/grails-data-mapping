package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Session

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
import grails.gorm.tests.Role
import grails.gorm.tests.User

class Setup {

    static session
    static datastore
    static transaction
    static storeDir

    static destroy() {
        transaction.rollback()
        session.nativeInterface.shutdown()
        new File(storeDir).deleteDir()
    }

    static Session setup(classes) {

        storeDir = File.createTempFile("neo4j",null)
        assert storeDir.delete()
        assert storeDir.mkdir()
        storeDir = storeDir.path
        datastore = new Neo4jDatastore(storeDir: storeDir)

        def ctx = new GenericApplicationContext()
        ctx.refresh()
        datastore.applicationContext = ctx

        classes << User << Role

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

        datastore.afterPropertiesSet()

        datastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        session = datastore.connect()
        transaction = session.beginTransaction()
        session
    }

}
