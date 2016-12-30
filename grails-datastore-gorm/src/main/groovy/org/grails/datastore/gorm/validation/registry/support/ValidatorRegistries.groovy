package org.grails.datastore.gorm.validation.registry.support

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.gorm.validation.javax.JavaxValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.validation.ValidatorRegistry
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource

/**
 * Utility methods for creating Validator registries
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class ValidatorRegistries {

    private ValidatorRegistries() {
    }

    /**
     * Creates the most appropriate validator registry
     *
     * @param mappingContext The mapping context
     * @param settings the settings
     * @param messageSource the message source
     * @return The registry
     */
    static ValidatorRegistry createValidatorRegistry(MappingContext mappingContext, ConnectionSourceSettings settings, MessageSource messageSource = new StaticMessageSource()) {
        ValidatorRegistry validatorRegistry;
        if(JavaxValidatorRegistry.isAvailable()) {
            validatorRegistry = new JavaxValidatorRegistry(mappingContext, settings, messageSource);
        }
        else {
            validatorRegistry = new DefaultValidatorRegistry(mappingContext, settings, messageSource);
        }
        return validatorRegistry;
    }
}
