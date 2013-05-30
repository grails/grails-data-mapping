package org.codehaus.groovy.grails.orm.hibernate

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod
import org.grails.datastore.gorm.GormValidationApi

@CompileStatic
class HibernateGormValidationApi<D> extends GormValidationApi<D> {

    private ClassLoader classLoader

    ValidatePersistentMethod validateMethod

    HibernateGormValidationApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)

        this.classLoader = classLoader

        def sessionFactory = datastore.getSessionFactory()

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof GrailsDomainClassMappingContext) {
            GrailsDomainClassMappingContext domainClassMappingContext = (GrailsDomainClassMappingContext)mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def validator = mappingContext.getEntityValidator(
                mappingContext.getPersistentEntity(persistentClass.name))
            validateMethod = new ValidatePersistentMethod(sessionFactory,
                classLoader, grailsApplication, validator, datastore)
        }
    }

    @Override
    boolean validate(D instance) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [] as Object[])
        }
        return super.validate(instance)
    }

    @Override
    boolean validate(D instance, boolean evict) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [evict] as Object[])
        }
        return super.validate(instance, evict)
    }

    @Override
    boolean validate(D instance, Map arguments) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [arguments] as Object[])
        }
        return super.validate(instance, arguments)
    }

    @Override
    boolean validate(D instance, List fields) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [fields] as Object[])
        }
        return super.validate(instance, fields)
    }
}
