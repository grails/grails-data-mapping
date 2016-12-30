package org.grails.datastore.gorm.validation.javax

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.ClassUtils
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.env.PropertyResolver
import org.springframework.validation.Validator
import org.springframework.validation.annotation.Validated
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator

import javax.validation.Validation
import javax.validation.ValidatorFactory

/**
 * A validator registry that creates validators
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class JavaxValidatorRegistry extends DefaultValidatorRegistry implements ValidatorFactory {

    /**
     * The validator factory
     */
    @Delegate final ValidatorFactory validatorFactory

    JavaxValidatorRegistry(MappingContext mappingContext, ConnectionSourceSettings settings, MessageSource messageSource = new StaticMessageSource()) {
        super(mappingContext, settings, messageSource)

        def validatorConfiguration = Validation.byDefaultProvider()
                .configure()
        validatorConfiguration.ignoreXmlConfiguration()
        validatorConfiguration.messageInterpolator(new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource)))

        validatorFactory = new GormValidatorFactoryAdapter( validatorConfiguration.buildValidatorFactory() )
    }

    @Override
    Validator getValidator(PersistentEntity entity) {
        def ann = entity.javaClass.getAnnotation(Validated)
        if(ann != null && isAvailable()) {
            return new GormValidatorAdapter(validatorFactory.getValidator())
        }
        else {
            return super.getValidator(entity)
        }
    }

    /**
     * @return Whether javax.validation is available
     */
    static boolean isAvailable() {
        ClassUtils.isPresent("javax.validation.Validation")
    }
}
