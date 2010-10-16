package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.jcr.JcrDatastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.validation.Validator
import org.springframework.validation.Errors
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.util.StringUtils
import org.grails.datastore.gorm.jcr.JcrGormEnhancer

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class Setup {
  static jcr
  static destroy() {
    jcr?.destroy()
  }
  static Session setup(classes) {

    jcr = new JcrDatastore()
    for(cls in classes) {
      jcr.mappingContext.addPersistentEntity(cls)
    }

    PersistentEntity entity = jcr.mappingContext.persistentEntities.find {
      PersistentEntity e -> e.name.contains("TestEntity")
    }

    redis.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if(!StringUtils.hasText(o.name)) {
                  errors.rejectValue("name", "name.is.blank")
                }
            }
    ] as Validator)

    def enhancer = new JcrGormEnhancer(jcr, new DatastoreTransactionManager(datastore: redis))
    enhancer.enhance()

    redis.mappingContext.addMappingContextListener({ e ->
      enhancer.enhance e
    } as MappingContext.Listener)


    def con = jcr.connect(conconnectionDetails,[username:"username",
                              password:"password",
                              workspace:"default",
                              configuration:"classpath:repository.xml",
                              homeDir:"/temp/repo"])
    return con
  }
}
