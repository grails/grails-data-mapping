package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValueMappingContext
import org.springframework.datastore.mapping.gemfire.GemfireDatastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.validation.Errors
import org.springframework.util.StringUtils
import org.springframework.validation.Validator
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.model.MappingContext
import com.gemstone.gemfire.cache.Cache
import org.grails.datastore.gorm.gemfire.GemfireGormEnhancer

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 5, 2010
 * Time: 3:33:34 PM
 * To change this template use File | Settings | File Templates.
 */
class Setup {
  static gemfire
  static destroy() {
    gemfire?.destroy()
  }
  static Session setup(classes) {
    gemfire = new GemfireDatastore(new KeyValueMappingContext(""), [:])
    gemfire.afterPropertiesSet()
    for(cls in classes) {
      gemfire.mappingContext.addPersistentEntity(cls)
    }

    PersistentEntity entity = gemfire.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

    gemfire.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if(!StringUtils.hasText(o.name)) {
                  errors.rejectValue("name", "name.is.blank")
                }
            }
    ] as Validator)

    def enhancer = new GemfireGormEnhancer(gemfire, new DatastoreTransactionManager(datastore: gemfire))
    enhancer.enhance()

    gemfire.mappingContext.addMappingContextListener({ e ->
      enhancer.enhance e
    } as MappingContext.Listener)


    def con = gemfire.connect()
    
    return con
  }
}
