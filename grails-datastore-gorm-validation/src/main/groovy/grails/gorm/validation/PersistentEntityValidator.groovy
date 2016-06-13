package grails.gorm.validation

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.proxy.ProxyHandler
import org.grails.datastore.mapping.reflect.EntityReflector
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * A Validator that validates a {@link org.grails.datastore.mapping.model.PersistentEntity} against known constraints
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class PersistentEntityValidator implements CascadingValidator, ConstrainedEntity {

    private static final List<String> EMBEDDED_EXCLUDES = Arrays.asList(
                                                            GormProperties.IDENTITY,
                                                            GormProperties.VERSION)

    final PersistentEntity entity
    final EntityReflector entityReflector
    final MessageSource messageSource
    final Class targetClass
    final Map<String, ConstrainedProperty> constrainedProperties

    protected final ProxyHandler proxyHandler

    PersistentEntityValidator(PersistentEntity entity, MessageSource messageSource, ConstraintsEvaluator constraintsEvaluator) {
        this.entity = entity
        this.messageSource = messageSource
        this.targetClass = entity.javaClass
        def mappingContext = entity.getMappingContext()
        this.entityReflector = mappingContext.getEntityReflector(entity)
        this.proxyHandler = mappingContext.getProxyHandler()

        def evaluated = constraintsEvaluator.evaluate(targetClass)
        this.constrainedProperties = Collections.unmodifiableMap(evaluated)
        if(constrainedProperties == null) {
            throw new IllegalStateException("Constraint evaluator returned null for class: $targetClass")
        }
    }

    @Override
    void validate(Object obj, Errors errors, boolean cascade = true) {
        if (obj == null || !targetClass.isInstance(obj)) {
            throw new IllegalArgumentException("Argument [$obj] is not an instance of [$targetClass] which this validator is configured for")
        }


        Map<String, ConstrainedProperty> constrainedProperties = this.constrainedProperties
        Set<String> constrainedPropertyNames = new HashSet<>(constrainedProperties.keySet())

        for(PersistentProperty pp in entity.persistentProperties) {
            def propertyName = pp.name

            ConstrainedProperty constrainedProperty = constrainedProperties.get(propertyName)

            if(constrainedProperty != null) {
                validatePropertyWithConstraint(obj, propertyName, entityReflector, errors, constrainedProperty)
            }

            if(pp instanceof Association) {
                Association association = (Association)pp
                if(association.isOwningSide()) {
                    cascadeToAssociativeProperty(obj, errors, entityReflector, association)
                }
            }

            constrainedPropertyNames.remove(propertyName)
        }

        for(String remainingProperty in constrainedPropertyNames) {
            ConstrainedProperty constrainedProperty = constrainedProperties.get(remainingProperty)
            if(remainingProperty != null) {
                validatePropertyWithConstraint(obj, remainingProperty, entityReflector, errors, constrainedProperty)
            }
        }

    }

    /**
     * Cascades validation onto an associative property maybe a one-to-many, one-to-one or many-to-one relationship.
     *
     * @param errors The Errors instnace
     * @param bean The original bean
     * @param association The associative property
     */
    protected void cascadeToAssociativeProperty(Object parent, Errors errors, EntityReflector reflector, Association association) {
        String propertyName = association.getName()
        if (errors.hasFieldErrors(propertyName)) {
            return
        }

        if (association instanceof ToOne) {
            Object associatedObject = reflector.getProperty(parent, propertyName)

            if(proxyHandler?.isInitialized(associatedObject)) {
                cascadeValidationToOne(parent, propertyName, (ToOne)association, errors, reflector, associatedObject, null)
            }
        }
        else if (association instanceof ToMany) {
            cascadeValidationToMany(parent, propertyName, association, errors, reflector)
        }
    }

    /**
     * Cascades validation to a one-to-many type relationship. Normally a collection such as a List or Set
     * each element in the association will also be validated.
     *
     * @param errors The Errors instance
     * @param entityReflector The entity reflector
     * @param association An association whose isOneToMeny() method returns true
     * @param propertyName The name of the property
     */
    @SuppressWarnings("rawtypes")
    protected void cascadeValidationToMany(Object parentObject, String propertyName, Association association, Errors errors, EntityReflector entityReflector) {

        Object collection = entityReflector.getProperty(parentObject, propertyName)
        if(collection == null || !proxyHandler?.isInitialized(collection)) {
            return
        }

        if (collection instanceof List || collection instanceof SortedSet) {
            int idx = 0;
            for (Object associatedObject : ((Collection)collection)) {
                cascadeValidationToOne(parentObject, propertyName, association, errors, entityReflector,associatedObject, idx++)
            }
        }
        else if (collection instanceof Collection) {
            Integer index = 0;
            for (Object associatedObject : ((Collection)collection)) {
                cascadeValidationToOne(parentObject, propertyName, association, errors, entityReflector,associatedObject, index++)
            }
        }
        else if (collection instanceof Map) {

            for (Object entryObject in ((Map) collection).entrySet()) {
                Map.Entry entry = (Map.Entry) entryObject;
                cascadeValidationToOne(parentObject, propertyName, association, errors, entityReflector, entry.value, entry.key)
            }
        }
    }
    /**
     * Cascades validation to a one-to-one or many-to-one property.
     *
     * @param errors The Errors instance
     * @param bean The original BeanWrapper
     * @param associatedObject The associated object's current value
     * @param association The GrailsDomainClassProperty instance
     * @param propertyName The name of the property
     * @param indexOrKey
     */
    @SuppressWarnings("rawtypes")
    protected void cascadeValidationToOne(Object parentObject, String propertyName, Association association, Errors errors, EntityReflector reflector, Object associatedObject, Object indexOrKey) {

        if (associatedObject == null) {
            return
        }

        PersistentEntity associatedEntity = association.getAssociatedEntity()

        MappingContext mappingContext = associatedEntity.getMappingContext()
        EntityReflector associatedReflector = mappingContext.getEntityReflector(associatedEntity)

        if (associatedEntity == null || !association.isOwningSide()) {
            return
        }

        Association otherSide = null
        if (association.isBidirectional()) {
            otherSide = association.getInverseSide()
        }

        Map associatedConstrainedProperties

        def validator = mappingContext.getEntityValidator(associatedEntity)
        if(validator instanceof PersistentEntityValidator) {
            associatedConstrainedProperties = ((PersistentEntityValidator)validator).getConstrainedProperties()
        }
        else {
            associatedConstrainedProperties = Collections.<String, ConstrainedProperty>emptyMap()
        }


        List<PersistentProperty> associatedPersistentProperties = associatedEntity.getPersistentProperties()
        String nestedPath = errors.getNestedPath()
        try {
            errors.setNestedPath(buildNestedPath(nestedPath, propertyName, indexOrKey));

            for (PersistentProperty associatedPersistentProperty : associatedPersistentProperties) {
                if (associatedPersistentProperty.equals(otherSide)) continue;
                if (association.isEmbedded() && EMBEDDED_EXCLUDES.contains(associatedPersistentProperty.getName())) {
                    continue
                }

                String associatedPropertyName = associatedPersistentProperty.getName();
                if (associatedConstrainedProperties.containsKey(associatedPropertyName)) {

                    ConstrainedProperty associatedConstrainedProperty = associatedConstrainedProperties.get(associatedPropertyName)
                    validatePropertyWithConstraint(associatedObject, errors.getNestedPath() + associatedPropertyName, associatedReflector, errors, associatedConstrainedProperty)
                }

                if (associatedPersistentProperty instanceof Association) {

                    cascadeToAssociativeProperty(
                            associatedObject,
                            errors,
                            associatedReflector,
                            (Association)associatedPersistentProperty)
                }
            }
        }
        finally {
            errors.setNestedPath(nestedPath)
        }
    }

    private String buildNestedPath(String nestedPath, String componentName, Object indexOrKey) {
        if (indexOrKey == null) {
            // Component is neither part of a Collection nor Map.
            return nestedPath + componentName;
        }

        if (indexOrKey instanceof Integer) {
            // Component is part of a Collection. Collection access string
            // e.g. path.object[1] will be appended to the nested path.
            return nestedPath + componentName + "[" + indexOrKey + "]";
        }

        // Component is part of a Map. Nested path should have a key surrounded
        // with apostrophes at the end.
        return nestedPath + componentName + "['" + indexOrKey + "']";
    }

    private void validatePropertyWithConstraint(Object obj, String propertyName, EntityReflector reflector, Errors errors, ConstrainedProperty constrainedProperty) {

        int i = propertyName.lastIndexOf(".")
        String constrainedPropertyName
        if (i > -1) {
            constrainedPropertyName = propertyName.substring(i + 1, propertyName.length())
        }
        else {
            constrainedPropertyName = propertyName
        }
        FieldError fieldError = errors.getFieldError(constrainedPropertyName)
        if (fieldError == null) {
            constrainedProperty.validate(obj, reflector.getProperty(obj, constrainedPropertyName), errors)
        }
    }
    @Override
    boolean supports(Class<?> clazz) {
        return targetClass.is(clazz)
    }
}
