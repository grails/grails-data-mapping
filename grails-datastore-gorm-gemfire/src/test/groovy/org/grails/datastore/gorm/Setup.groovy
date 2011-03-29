package org.grails.datastore.gorm

import org.grails.datastore.gorm.gemfire.GemfireGormEnhancer
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.gemfire.GemfireDatastore
import org.springframework.datastore.mapping.gemfire.config.GormGemfireMappingFactory
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import com.gemstone.gemfire.cache.DataPolicy

/**
 * @author graemerocher
 */
class Setup {

    static gemfire

    static destroy() {
        gemfire?.destroy()
    }

    static Session setup(classes) {
        def context = new KeyValueMappingContext("")
        def factory = new GormGemfireMappingFactory()
        factory.defaultDataPolicy = DataPolicy.REPLICATE
        context.mappingFactory = factory
        gemfire = new GemfireDatastore(context)
        gemfire.afterPropertiesSet()
        for (cls in classes) {
            gemfire.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = gemfire.mappingContext.persistentEntities.find {
            PersistentEntity e -> e.name.contains("TestEntity")
        }

        gemfire.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def enhancer = new GemfireGormEnhancer(gemfire,
            new DatastoreTransactionManager(datastore: gemfire))
        enhancer.enhance()

        gemfire.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)


        gemfire.connect()
    }
}
