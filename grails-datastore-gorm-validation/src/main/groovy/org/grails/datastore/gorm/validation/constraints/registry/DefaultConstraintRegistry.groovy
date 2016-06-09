package org.grails.datastore.gorm.validation.constraints.registry

import grails.gorm.validation.Constraint
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.factory.ConstraintFactory
import org.grails.datastore.gorm.validation.constraints.factory.DefaultConstraintFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.util.concurrent.ConcurrentHashMap


/**
 * Default implementation of the {@link ConstraintRegistry} interface. Provides lookup and registration of constraints
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class DefaultConstraintRegistry implements ConstraintRegistry {

    protected Map<String, List<ConstraintFactory>> factoriesByName = new ConcurrentHashMap<String, List<ConstraintFactory>>().withDefault { String name ->
        return []
    }
    protected Map<Class<? extends Constraint>, List<ConstraintFactory>> factoriesByType = new ConcurrentHashMap<Class<? extends Constraint>, List<ConstraintFactory>>().withDefault { Class<? extends Constraint> type ->
        return []
    }

    protected final MessageSource messageSource

    DefaultConstraintRegistry(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @Autowired(required = false)
    void setConstraintFactories(ConstraintFactory[] constraintFactories) {
        for(factory in constraintFactories) {
            addConstraintFactory(factory)
        }
    }

    @Override
    void addConstraintFactory(ConstraintFactory factory) {
        factoriesByType.get( factory.type ).add(factory)
        factoriesByName.get( factory.name).add(factory)
    }

    @Override
    void addConstraint(Class<? extends Constraint> constraintClass, Class targetPropertyType = Object) {
        addConstraintFactory(new DefaultConstraintFactory(constraintClass, messageSource, targetPropertyType))
    }

    @Override
    List<ConstraintFactory> findConstraintFactories(String name) {
        return factoriesByName.get(name)
    }

    @Override
    def <T extends Constraint> List<ConstraintFactory<T>> findConstraintFactories(Class<T> constraintType) {
        return factoriesByType.get(constraintType)
    }
}
