package org.grails.datastore.mapping.validation

import spock.lang.Specification

/**
 * Tests for validation errors object
 */
class ValidationErrorsSpec extends Specification{

    void "Test retrieve errors using subscript operator"() {
        given:"A validation errors object"
            def errors = new ValidationErrors(new Person())

        when:"errors are stored"
            errors['name'] = "error.code"

        then:"They can be retrieved"
            errors['name'].code == 'error.code'
    }
}
class Person { String name }
