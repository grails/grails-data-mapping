/*
 * Copyright 2015 the original author or authors.
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
package org.grails.datastore.gorm

import grails.validation.CascadingValidator
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.grails.datastore.mapping.engine.event.ValidationEvent
import org.grails.datastore.mapping.validation.ValidationErrors
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.validation.Validator

/**
 *
 * @author Jeff Brown
 * @since 4.0
 */
trait ValidationMethods {
    private static GormValidationApi internalValidationApi
    Errors errors
    
    static void initInternalValidationApi(GormValidationApi gvi) {
        internalValidationApi = gvi
    }

    static GormValidationApi currentGormValidationApi() {
        if(internalValidationApi == null) {
            throw new IllegalStateException("Method on class [${this.getName()}] was used outside of a Grails application. If running in the context of a test using the mocking API or bootstrap Grails correctly.")
        }
        internalValidationApi
    }

    /**
     * Validates an instance for the given arguments
     *
     * @param arguments The arguments to use
     * @return True if the instance is valid
     */
    boolean validate(Map arguments) {
        currentGormValidationApi().validate this, arguments
    }

    /**
     * Validates an instance
     *
     * @param fields The list of fields to validate
     * @return True if the instance is valid
     */
    boolean validate(List fields) {
        currentGormValidationApi().validate this, fields
    }

    /**
     * Validates an instance
     *
     * @return True if the instance is valid
     */
    boolean validate() {
        currentGormValidationApi().validate this
    }

    /**
     * Obtains the errors for an instance
     * @return The {@link Errors} instance
     */
    Errors getErrors() {
        if(errors == null) {
            errors = new ValidationErrors(this)
        }
        errors
    }

    /**
     * Clears any errors that exist on an instance
     */
    void clearErrors() {
        errors = new ValidationErrors(this)
    }

    /**
     * Tests whether an instance has any errors
     * @return True if errors exist
     */
    Boolean hasErrors() {
        errors?.hasErrors()
    }
}