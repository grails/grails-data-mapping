/* Copyright (C) 2010 SpringSource
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

import org.springframework.datastore.core.Datastore
import org.springframework.validation.Validator
import org.springframework.datastore.mapping.MappingContext
import org.springframework.validation.Errors
import org.springframework.validation.BeanPropertyBindingResult

/**
 * Methods used for validating GORM instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class GormValidationApi extends AbstractGormApi{

  static final ERRORS_ATTR = "org.codehaus.groovy.grails.ERRORS"

  Validator validator

  GormValidationApi(Class persistentClass, Datastore datastore) {
    super(persistentClass, datastore);
    MappingContext context = datastore.mappingContext
    def entity = context.getPersistentEntity(persistentClass.name)
    validator = context.getEntityValidator(entity)
  }

  /**
   * Validates an instance
   *
   * @param instance The instance to validate
   * @return True if the instance is valid
   */
  boolean validate(instance) {
    if(validator) {
      Errors errors = getErrors(instance)
      validator.validate instance, errors
      return !errors.hasErrors()
    }
    return true
  }

  /**
   * Obtains the errors for an instance
   * @param instance The instance to obtain errors for
   * @return The {@link Errors} instance
   */
  Errors getErrors(instance) {
    def session = datastore.currentSession
    def errors = session.getAttribute(instance, ERRORS_ATTR)
    if(errors == null) {
      errors = resetErrors(instance)
    }
    return errors
  }

  private Errors resetErrors(instance) {
    def er = new BeanPropertyBindingResult(instance, persistentClass.name)
    setErrors(instance, er)
    return er
  }

  /**
   * Sets the errors for an instance
   * @param instance The instance
   * @param errors The errors
   */
  void setErrors(instance, Errors errors) {
    def session = datastore.currentSession

    session.setAttribute(instance, ERRORS_ATTR, errors)
  }

  /**
   * Clears any errors that exist on an instance
   * @param instance The instance
   */
  void clearErrors(instance) {
    resetErrors(instance)
  }

  /**
   * Tests whether an instance has any errors
   * @param instance The instance
   * @return True if errors exist
   */
  boolean hasErrors(instance) {
     instance.errors?.hasErrors()
  }
}
