package org.grails.datastore.gorm

import org.grails.datastore.gorm.jpa.JpaGormEnhancer
import org.hibernate.dialect.HSQLDialect
import org.hibernate.ejb.Ejb3Configuration
import org.hsqldb.jdbcDriver
import org.springframework.context.support.GenericApplicationContext
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.jpa.JpaDatastore
import org.springframework.datastore.mapping.jpa.config.JpaMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class Setup {

    static jpaDatastore
    static trans

    static destroy() {
        trans.commit()
        JpaDatastore.retrieveSession().disconnect()
    }

    static Session setup(classes) {

        def config = new Ejb3Configuration()
        config.setProperty "hibernate.dialect", HSQLDialect.name
        config.setProperty "hibernate.connection.driver_class", jdbcDriver.name
        config.setProperty "hibernate.connection.url", "jdbc:hsqldb:mem:devDB"
        config.setProperty "hibernate.connection.username", "sa"
        config.setProperty "hibernate.connection.password", ""
        config.setProperty "hibernate.hbm2ddl.auto", "create-drop"
        config.setProperty "hibernate.show_sql", "true"
        config.setProperty "hibernate.format_sql", "true"

        def context = new JpaMappingContext()
        for (Class c in classes) {
            config.addAnnotatedClass c
            context.addPersistentEntity c
        }

        def entityManagerFactory = config.buildEntityManagerFactory()

        def txMgr = new JpaTransactionManager(entityManagerFactory)

        def ctx = new GenericApplicationContext()
        ctx.refresh()
        jpaDatastore = new JpaDatastore(context, entityManagerFactory, txMgr, ctx)

        PersistentEntity entity = jpaDatastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

            jpaDatastore.mappingContext.addEntityValidator(entity, [
                    supports: { Class c -> true },
                    validate: { Object o, Errors errors ->
                        if (!StringUtils.hasText(o.name)) {
                          errors.rejectValue("name", "name.is.blank")
                        }
                    }
            ] as Validator)

        def enhancer = new JpaGormEnhancer(jpaDatastore, txMgr)
        enhancer.enhance()

        jpaDatastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        def session = jpaDatastore.connect()

        trans = session.beginTransaction()
        return session
    }
}
