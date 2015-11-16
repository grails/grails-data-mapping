/* Copyright 2004-2005 Graeme Rocher
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
package org.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.grails.datastore.gorm.proxy.ProxyHandlerAdapter;
import org.grails.datastore.gorm.support.BeforeValidateHelper;
import org.grails.datastore.gorm.validation.CascadingValidator;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.proxy.SimpleHibernateProxyHandler;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * First checks if the Hibernate PersistentCollection instance has been initialised before bothering
 * to cascade.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class HibernateDomainClassValidator extends GrailsDomainClassValidator implements MessageSourceAware, CascadingValidator, InitializingBean{

    private BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper();
    private ProxyHandler proxyHandler = new ProxyHandlerAdapter(new SimpleHibernateProxyHandler());
    private AbstractHibernateDatastore hibernateDatastore;

    @Override
    protected GrailsDomainClass getAssociatedDomainClassFromApplication(Object associatedObject) {
        String associatedObjectType = Hibernate.getClass(associatedObject).getName();
        return (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, associatedObjectType);
    }

    @Autowired(required = false)
    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    @Override
    public void validate(final Object obj, final Errors errors, final boolean cascade) {
        hibernateDatastore.withFlushMode( AbstractHibernateDatastore.FlushMode.MANUAL, new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                HibernateDomainClassValidator.super.validate(obj, errors, cascade);
                return !errors.hasErrors();
            }
        });

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
            associatedObject = proxyHandler.isProxy(associatedObject) ? proxyHandler.unwrapIfProxy(associatedObject) : associatedObject;
            if(associatedObject != null) {
                cascadeBeforeValidate(associatedObject);
                super.cascadeValidationToOne(errors, bean, associatedObject, persistentProperty, propertyName, indexOrKey);
            }
        }
    }

    protected void cascadeBeforeValidate(Object associatedObject) {
        final GrailsDomainClass associatedDomainClass = getAssociatedDomainClassFromApplication(associatedObject);
        if(associatedDomainClass != null) {
            beforeValidateHelper.invokeBeforeValidate(associatedObject, new ArrayList<Object>(associatedDomainClass.getConstrainedProperties().keySet()));
        }
    }

    public void setHibernateDatastore(AbstractHibernateDatastore hibernateDatastore) {
        this.hibernateDatastore = hibernateDatastore;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(hibernateDatastore != null) {
            MappingContext mappingContext = hibernateDatastore.getMappingContext();
            PersistentEntity mappedEntity = mappingContext.getPersistentEntity(getDomainClass().getFullName());
            if(mappedEntity != null) {
                mappingContext.addEntityValidator(mappedEntity, this);
            }
        }
    }
}
