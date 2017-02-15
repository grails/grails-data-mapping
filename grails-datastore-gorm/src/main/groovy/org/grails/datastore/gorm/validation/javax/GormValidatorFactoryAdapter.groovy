package org.grails.datastore.gorm.validation.javax

import groovy.transform.CompileStatic

import javax.validation.ConstraintValidatorFactory
import javax.validation.MessageInterpolator
import javax.validation.ParameterNameProvider
import javax.validation.TraversableResolver
import javax.validation.Validator
import javax.validation.ValidatorContext
import javax.validation.ValidatorFactory

/**
 * A ValidatorFactory that creates adapted validators
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class GormValidatorFactoryAdapter implements ValidatorFactory  {

    final ValidatorFactory factory

    GormValidatorFactoryAdapter(ValidatorFactory factory) {
        this.factory = factory
    }

    @Override
    Validator getValidator() {
        return new GormValidatorAdapter(factory.getValidator())
    }

    @Override
    void close() {
        factory.close()
    }

    @Override
    ValidatorContext usingContext() {
        return new GormValidatorContext(factory.usingContext())
    }

    @Override
    MessageInterpolator getMessageInterpolator() {
        factory.getMessageInterpolator()
    }

    @Override
    TraversableResolver getTraversableResolver() {
        return factory.getTraversableResolver()
    }

    @Override
    ConstraintValidatorFactory getConstraintValidatorFactory() {
        return factory.getConstraintValidatorFactory()
    }

    @Override
    ParameterNameProvider getParameterNameProvider() {
        return factory.getParameterNameProvider()
    }

    @Override
    def <T> T unwrap(Class<T> type) {
        return factory.unwrap(type)
    }

    static class GormValidatorContext implements ValidatorContext {
        final ValidatorContext delegate

        GormValidatorContext(ValidatorContext delegate) {
            this.delegate = delegate
        }

        @Override
        ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator) {
            delegate.messageInterpolator(messageInterpolator)
            return this
        }

        @Override
        ValidatorContext traversableResolver(TraversableResolver traversableResolver) {
            delegate.traversableResolver(traversableResolver)
            return this
        }

        @Override
        ValidatorContext constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
            delegate.constraintValidatorFactory(constraintValidatorFactory)
            return this
        }

        @Override
        ValidatorContext parameterNameProvider(ParameterNameProvider parameterNameProvider) {
            delegate.parameterNameProvider(parameterNameProvider)
            return this
        }

        @Override
        Validator getValidator() {
            return new GormValidatorAdapter( delegate.getValidator() )
        }
    }
}
