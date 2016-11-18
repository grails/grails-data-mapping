package org.grails.datastore.gorm.validation.constraints.builtin

import grails.gorm.DetachedCriteria
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.validation.constraints.AbstractConstraint
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.datastore.mapping.reflect.EntityReflector
import org.springframework.context.MessageSource
import org.springframework.validation.Errors

/**
 * A constraint that validates for the presence of an existing object (uniqueness)
 *
 * @author Graeme Rocher
 * @since 6.0
 *
 */
@CompileStatic
class UniqueConstraint extends AbstractConstraint {

    public static final String NAME = "unique"

    protected List group = []

    UniqueConstraint(Class<?> constraintOwningClass, String constraintPropertyName, Object constraintParameter, MessageSource messageSource) {
        super(constraintOwningClass, constraintPropertyName, constraintParameter, messageSource)
        if(constraintParameter instanceof Iterable) {
            for(property in ((Iterable)constraintParameter)) {
                group.add(property.toString())
            }
        }
        else if(constraintParameter instanceof CharSequence) {
            group.add(constraintParameter.toString())
        }
    }

    @Override
    protected Object validateParameter(Object constraintParameter) {
        return constraintParameter instanceof Boolean || constraintParameter instanceof Iterable || constraintParameter instanceof CharSequence
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {

        DetachedCriteria detachedCriteria = new DetachedCriteria(constraintOwningClass)
        EntityReflector reflector = detachedCriteria.getPersistentEntity().getReflector()
        String constraintPropertyName = this.constraintPropertyName
        List group = this.group

        detachedCriteria = detachedCriteria.build {
            eq(constraintPropertyName, propertyValue)
            if(!group.isEmpty()) {
                for(prop in group) {
                    def propName = prop.toString()
                    def value = reflector.getProperty(target, propName)
                    if(value != null) {
                        eq propName, value
                    }
                }
            }
        }.id()

        def existingId = detachedCriteria.get()
        if(existingId != null) {
            def targetId = reflector.getIdentifier(target)
            if(targetId != existingId) {
                def args = [constraintPropertyName, constraintOwningClass, propertyValue] as Object[]
                rejectValue(target, errors, "unique", args, getDefaultMessage("default.not.unique.message"))
            }
        }
    }

    @Override
    boolean supports(Class type) {
        return true
    }

    @Override
    String getName() {
        return NAME
    }
}
