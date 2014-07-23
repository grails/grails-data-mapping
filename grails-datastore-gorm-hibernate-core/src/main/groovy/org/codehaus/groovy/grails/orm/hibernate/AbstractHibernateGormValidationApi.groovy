/* 
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate

import grails.validation.ValidationErrors
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.orm.hibernate.validation.AbstractPersistentConstraint
import org.codehaus.groovy.grails.validation.CascadingValidator
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.mapping.engine.event.ValidationEvent
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

@CompileStatic
abstract class AbstractHibernateGormValidationApi<D> extends GormValidationApi<D> {

    public static final String ARGUMENT_DEEP_VALIDATE = "deepValidate";
    private static final String ARGUMENT_EVICT = "evict";


    protected ClassLoader classLoader
    protected AbstractHibernateDatastore datastore
    protected IHibernateTemplate hibernateTemplate

    protected AbstractHibernateGormValidationApi(Class<D> persistentClass, AbstractHibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)
        this.classLoader = classLoader
        this.datastore = datastore
    }


    @Override
    boolean validate(D instance, Map arguments = Collections.emptyMap()) {
        validate(instance, null, arguments)
    }

    boolean validate(D instance, List validatedFieldsList, Map arguments = Collections.emptyMap()) {
        Errors errors = setupErrorsProperty(instance);
        if(validator == null) return true

        Boolean valid = Boolean.TRUE
        // should evict?
        boolean evict = false
        boolean deepValidate = true
        Set validatedFields = null
        if(validatedFieldsList != null) {
            validatedFields = new HashSet(validatedFieldsList)
        }

        if (arguments.containsKey(ARGUMENT_DEEP_VALIDATE)) {
            deepValidate = GrailsClassUtils.getBooleanFromMap(ARGUMENT_DEEP_VALIDATE, arguments)
        }

        evict = GrailsClassUtils.getBooleanFromMap(ARGUMENT_EVICT, arguments);

        fireEvent instance, validatedFieldsList

        AbstractPersistentConstraint.sessionFactory.set datastore.sessionFactory
        try {
            if (deepValidate && (validator instanceof CascadingValidator)) {
                ((CascadingValidator)validator).validate instance, errors, deepValidate
            }
            else {
                validator.validate instance, errors
            }
        }
        finally {
            AbstractPersistentConstraint.sessionFactory.remove()
        }

        int oldErrorCount = errors.errorCount
        errors = filterErrors(errors, validatedFields, instance)

        if (errors.hasErrors()) {
            valid = Boolean.FALSE
            if (evict) {
                // if an boolean argument 'true' is passed to the method
                // and validation fails then the object will be evicted
                // from the session, ensuring it is not saved later when
                // flush is called
                if (hibernateTemplate.contains(instance)) {
                    hibernateTemplate.evict(instance)
                }
            }
        }

        // If the errors have been filtered, update the 'errors' object attached to the target.
        if (errors.errorCount != oldErrorCount) {
            MetaClass metaClass = GroovySystem.metaClassRegistry.getMetaClass(instance.getClass())
            metaClass.setProperty( instance, GrailsDomainClassProperty.ERRORS, errors)
        }

        return valid
    }

    private void fireEvent(Object target, List<?> validatedFieldsList) {
        ValidationEvent event = new ValidationEvent(datastore, target);
        event.setValidatedFields(validatedFieldsList);
        datastore.getApplicationContext().publishEvent(event);
    }

    @SuppressWarnings("rawtypes")
    private Errors filterErrors(Errors errors, Set validatedFields, Object target) {
        if (validatedFields == null) return errors;

        ValidationErrors result = new ValidationErrors(target);

        final List allErrors = errors.getAllErrors();
        for (Object allError : allErrors) {
            ObjectError error = (ObjectError) allError;

            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                if (!validatedFields.contains(fieldError.getField())) continue;
            }

            result.addError(error);
        }

        return result;
    }

    /**
     * Initializes the Errors property on target.  The target will be assigned a new
     * Errors property.  If the target contains any binding errors, those binding
     * errors will be copied in to the new Errors property.
     *
     * @param target object to initialize
     * @return the new Errors object
     */
    protected Errors setupErrorsProperty(Object target) {
        MetaClass mc = GroovySystem.metaClassRegistry.getMetaClass(target.getClass())

        def errors = new ValidationErrors(target)

        Errors originalErrors = (Errors) mc.getProperty(target, GrailsDomainClassProperty.ERRORS)
        for (Object o in originalErrors.fieldErrors) {
            FieldError fe = (FieldError)o
            if (fe.isBindingFailure()) {
                errors.addError(new FieldError(fe.getObjectName(),
                        fe.field,
                        fe.rejectedValue,
                        fe.bindingFailure,
                        fe.codes,
                        fe.arguments,
                        fe.defaultMessage))
            }
        }

        mc.setProperty(target, GrailsDomainClassProperty.ERRORS, errors);
        return errors;
    }
}
