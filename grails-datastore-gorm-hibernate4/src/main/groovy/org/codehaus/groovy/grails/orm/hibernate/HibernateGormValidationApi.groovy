package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod
import org.grails.datastore.gorm.GormValidationApi

class HibernateGormValidationApi extends GormValidationApi {

    private ClassLoader classLoader

    ValidatePersistentMethod validateMethod

    HibernateGormValidationApi(Class persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)

        this.classLoader = classLoader

        def sessionFactory = datastore.getSessionFactory()

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof GrailsDomainClassMappingContext) {
            GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def validator = mappingContext.getEntityValidator(
                    mappingContext.getPersistentEntity(persistentClass.name))
            validateMethod = new ValidatePersistentMethod(sessionFactory,
                    classLoader, grailsApplication, validator, datastore)
        }
    }

    @Override
    boolean validate(instance) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [] as Object[])
        }
        return super.validate(instance)
    }

    @Override
    boolean validate(instance, boolean evict) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [evict] as Object[])
        }
        return super.validate(instance, evict)
    }

    @Override
    boolean validate(instance, Map arguments) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [arguments] as Object[])
        }
        return super.validate(instance, arguments)
    }

    @Override
    boolean validate(instance, List fields) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [fields] as Object[])
        }
        return super.validate(instance, arguments)
    }
}
