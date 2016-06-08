package org.grails.datastore.mapping.validation;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.validation.Validator;

/**
 * Looks up validators from Spring
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class BeanFactoryValidatorRegistry implements ValidatorRegistry {
    private final BeanFactory beanFactory;

    public BeanFactoryValidatorRegistry(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Validator getValidator(PersistentEntity entity) {
        String validatorName = entity.getName() + "Validator";
        if(beanFactory.containsBean(validatorName)) {
            return beanFactory.getBean(validatorName, Validator.class);
        }
        return null;
    }
}
