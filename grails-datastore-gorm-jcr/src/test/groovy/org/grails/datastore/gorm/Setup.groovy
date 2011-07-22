package org.grails.datastore.gorm

import org.grails.datastore.gorm.jcr.JcrGormEnhancer
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.jcr.JcrDatastore
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

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
        for (cls in classes) {
            jcr.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = jcr.mappingContext.persistentEntities.find {
            PersistentEntity e -> e.name.contains("TestEntity")
        }

        jcr.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def enhancer = new JcrGormEnhancer(jcr, new DatastoreTransactionManager(datastore: jcr))
        enhancer.enhance()

        jcr.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        jcr.connect([username:"username",
                     password:"password",
                     workspace:"default",
                     configuration:"classpath:repository.xml",
                     homeDir:"/temp/repo"])
    }
}
