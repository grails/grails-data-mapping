/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.validation;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.core.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.validation.GrailsDomainClassValidator;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.validation.Errors;

/**
 * First checks if the Hibernate PersistentCollection instance has been initialised before bothering
 * to cascade.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class HibernateDomainClassValidator extends GrailsDomainClassValidator implements MessageSourceAware {

    private SessionFactory sessionFactory;
    private ProxyHandler proxyHandler = new HibernateProxyHandler();

    @Autowired(required = false)
    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }


    @Override
    protected GrailsDomainClass getAssociatedDomainClassFromApplication(Object associatedObject) {
        String associatedObjectType = Hibernate.getClass(associatedObject).getName();
        return (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, associatedObjectType);
    }

    @Override
    public void validate(Object obj, Errors errors, boolean cascade) {
        final Session session = sessionFactory.getCurrentSession();
        FlushMode previousMode = null;
        try {
            if (session != null) {
                previousMode = session.getFlushMode();
                session.setFlushMode(FlushMode.MANUAL);
            }

            super.validate(obj, errors, cascade);
        }
        finally {
            if (session != null && previousMode != null && !errors.hasErrors()) {
                session.setFlushMode(previousMode);
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
     */
    @Override
    protected void cascadeValidationToMany(Errors errors, BeanWrapper bean, GrailsDomainClassProperty persistentProperty, String propertyName) {
        Object collection = bean.getPropertyValue(propertyName);
        if (collection == null) {
            return;
        }

        if (collection instanceof PersistentCollection) {
            PersistentCollection persistentCollection = (PersistentCollection)collection;
            if (persistentCollection.wasInitialized()) {
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
            super.cascadeValidationToOne(errors, bean, associatedObject, persistentProperty, propertyName, indexOrKey);
        }
    }


    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
