package org.grails.datastore.gorm.validation.javax

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.validation.ValidationErrors

import javax.validation.ConstraintViolationException

/**
 * Utility methods for handling ConstraintViolationException
 *
 * @author Graeme Rocher
 * @since 6.1.3
 */
@CompileStatic
class ConstraintViolationUtils {

    /**
     * Converts a ConstraintViolationException to errors
     *
     * @param object The validated object
     * @param e The exception
     * @return The errors
     */
    static ValidationErrors asErrors(Object object, ConstraintViolationException e) {
        ValidationErrors errors = new ValidationErrors(object)
        for (violation in e.constraintViolations) {
            String property = violation.propertyPath.last().name
            String code = "${object.getClass().simpleName}.${violation.propertyPath}"
            String message = "${property} $violation.message"
            if(object.hasProperty(property)) {
                errors.rejectValue(property, code,[violation.invalidValue] as Object[], message)
            }
            else {
                errors.reject(code, [violation.invalidValue] as Object[], message)
            }
        }
        return errors
    }
}
