package org.grails.datastore.gorm.validation;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.datastore.gorm.support.BeforeValidateHelper;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.proxy.JavassistProxyFactory;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.grails.validation.GrailsDomainClassValidator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSourceAware;
import org.springframework.validation.Errors;

import javax.persistence.FlushModeType;
import java.util.ArrayList;

/**
 * A validator aware of proxies and that integrates with Grails' cascading validation
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class DefaultDomainClassValidator extends GrailsDomainClassValidator implements MessageSourceAware, CascadingValidator, ApplicationContextAware {

    private final BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper();
    private ProxyFactory proxyHandler = new JavassistProxyFactory();
    private Datastore datastore;
    private String datastoreName;
    private ApplicationContext applicationContext;

    public BeforeValidateHelper getBeforeValidateHelper() {
        return beforeValidateHelper;
    }

    public void setDatastoreName(String datastoreName) {
        this.datastoreName = datastoreName;
    }

    public Datastore getDatastore() {
        if(datastoreName == null) {
            throw new IllegalStateException("Not datastore name configured. Please provide one");
        }
        if(datastore != null) {
            return datastore;
        }
        else {
            datastore = applicationContext.getBean(datastoreName, Datastore.class);
            return datastore;
        }
    }

    @Override
    protected GrailsDomainClass getAssociatedDomainClassFromApplication(Object associatedObject) {
        String associatedObjectType = proxyHandler.getProxiedClass(associatedObject).getName();
        return (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, associatedObjectType);
    }

    @Autowired(required = false)
    public void setProxyFactory(ProxyFactory proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    @Override
    public void validate(final Object obj, final Errors errors, final boolean cascade) {
        Session currentSession = getDatastore().getCurrentSession();
        FlushModeType originalFlushMode = currentSession.getFlushMode();
        boolean hasErrors = false;
        try {
            currentSession.setFlushMode(FlushModeType.COMMIT);
            super.validate(obj, errors, cascade);
            hasErrors = errors.hasErrors();
        } finally {
            if(!hasErrors) {
                currentSession.setFlushMode(originalFlushMode);
            }
        }
    }

    /**
     * Overrides the default behaviour and first checks if a PersistentCollection instance has been initialised using the
     * wasInitialised() method before cascading
     *
     * @param errors The Spring Errors instance
     * @param bean The BeanWrapper for the bean
     * @param persistentProperty The GrailsDomainClassProperty instance
     * @param propertyName The name of the property
     *
     * @see org.hibernate.collection.PersistentCollection#wasInitialized()
     */
    @Override
    protected void cascadeValidationToMany(Errors errors, BeanWrapper bean, GrailsDomainClassProperty persistentProperty, String propertyName) {
        Object collection = bean.getPropertyValue(propertyName);
        if (collection == null) {
            return;
        }

        if (proxyHandler.isProxy(collection)) {
            if (proxyHandler.isInitialized(collection)) {
                super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
            }
        }
        else {
            super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
        }
    }

    @Override
    protected void cascadeValidationToOne(Errors errors, BeanWrapper bean, Object associatedObject, GrailsDomainClassProperty persistentProperty, String propertyName, Object indexOrKey) {
        if(proxyHandler.isInitialized(associatedObject)) {
            associatedObject = proxyHandler.isProxy(associatedObject) ? proxyHandler.unwrap(associatedObject) : associatedObject;
            if(associatedObject != null) {
                cascadeBeforeValidate(associatedObject);
                super.cascadeValidationToOne(errors, bean, associatedObject, persistentProperty, propertyName, indexOrKey);
            }
        }
    }

    protected void cascadeBeforeValidate(Object associatedObject) {
        final GrailsDomainClass associatedDomainClass = getAssociatedDomainClassFromApplication(associatedObject);
        if(associatedDomainClass != null) {
            getBeforeValidateHelper().invokeBeforeValidate(associatedObject, new ArrayList<Object>(associatedDomainClass.getConstrainedProperties().keySet()));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

