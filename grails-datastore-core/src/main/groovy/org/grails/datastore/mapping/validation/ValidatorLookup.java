package org.grails.datastore.mapping.validation;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.validation.Validator;

/**
 * Strategy interface for looking up validators
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ValidatorLookup {

    /**
     * Looks up a validator for the given entity
     *
     * @param entity The entity
     * @return The validator
     */
    Validator getValidator(PersistentEntity entity);
}
