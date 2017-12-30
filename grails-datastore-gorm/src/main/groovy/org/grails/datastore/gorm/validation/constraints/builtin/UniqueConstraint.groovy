package org.grails.datastore.gorm.validation.constraints.builtin

import grails.gorm.DetachedCriteria
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.validation.constraints.AbstractConstraint
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
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

        MappingContext mappingContext = detachedCriteria.getPersistentEntity()
                                                        .getMappingContext()
        PersistentEntity targetEntity = mappingContext.getPersistentEntity(mappingContext.getProxyHandler().getProxiedClass(target).getName())
        if(targetEntity == null) {
            throw new IllegalStateException("Cannot validate object [$target]. It is not a persistent entity")
        }

        EntityReflector reflector = targetEntity.reflector
        String constraintPropertyName = this.constraintPropertyName
        List group = this.group

        PersistentProperty persistentProperty = targetEntity.getPropertyByName(constraintPropertyName)
        boolean isToOne = persistentProperty instanceof ToOne
        if(isToOne) {
            def associationId = ((Association) persistentProperty).getAssociatedEntity().getReflector().getIdentifier(propertyValue)
            if(associationId == null) {
                // unsaved entity
                return
            }
        }

        detachedCriteria = detachedCriteria.build {
            eq(constraintPropertyName, propertyValue)
            if(!group.isEmpty()) {
                for(prop in group) {
                    String propName = prop.toString()
                    def value = reflector.getProperty(target, propName)
                    if(value != null) {
                        PersistentProperty associated = targetEntity.getPropertyByName(propName)
                        if(associated instanceof ToOne) {
                            def associationId = ((Association) associated).getAssociatedEntity().getReflector().getIdentifier(value)
                            if(associationId == null) {
                                continue
                            }
                        }
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
