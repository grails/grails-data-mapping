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

import grails.core.GrailsApplication;
import grails.core.GrailsDomainClass;
import org.grails.orm.hibernate.AbstractGrailsHibernateDomainClass;
import org.grails.orm.hibernate.IHibernateTemplate;
import org.codehaus.groovy.grails.validation.AbstractConstraint;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.lifecycle.ShutdownOperations;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Constraints that require access to the HibernateTemplate should subclass this class.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public abstract class AbstractPersistentConstraint extends AbstractConstraint implements PersistentConstraint {

    public static ThreadLocal<SessionFactory> sessionFactory = new ThreadLocal<SessionFactory>();

    static {
        ShutdownOperations.addOperation(new Runnable() {
            public void run() {
                sessionFactory.remove();
            }
        });
    }

    protected ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public abstract IHibernateTemplate getHibernateTemplate();

    /**
     * Returns whether the constraint supports being applied against the specified type;
     *
     * @param type The type to support
     * @return true if the constraint can be applied against the specified type
     */
    public boolean supports(@SuppressWarnings("rawtypes") Class type) {
        return true;
    }

    /**
     * Return whether the constraint is valid for the owning class
     *
     * @return true if it is
     */
    public boolean isValid() {
        if (applicationContext.containsBean("sessionFactory")) {
            GrailsApplication grailsApplication = applicationContext.getBean(
                    GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(
                    DomainClassArtefactHandler.TYPE, constraintOwningClass.getName());
            if (domainClass != null) {
                String mappingStrategy = domainClass.getMappingStrategy();
                return mappingStrategy.equals(GrailsDomainClass.GORM)
                    || mappingStrategy.equals(AbstractGrailsHibernateDomainClass.HIBERNATE);
            }
        }
        return false;
    }
}
