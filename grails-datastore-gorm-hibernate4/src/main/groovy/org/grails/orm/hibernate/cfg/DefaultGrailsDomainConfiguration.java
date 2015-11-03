/* Copyright 2004-2005 the original author or authors.
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
package org.grails.orm.hibernate.cfg;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import groovy.lang.Closure;
import groovy.util.Eval;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.grails.orm.hibernate.proxy.GroovyAwarePojoEntityTuplizer;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.Settings;
import org.hibernate.service.ServiceRegistry;

/**
 * Creates runtime configuration mapping for the Grails domain classes
 * based on the work done in the Hibernate Annotations project
 *
 * @author Graeme Rocher
 * @since 06-Jul-2005
 */
public class DefaultGrailsDomainConfiguration extends Configuration implements GrailsDomainConfiguration {

    private static final long serialVersionUID = -7115087342689305517L;
    private GrailsApplication grailsApplication;
    private Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    private boolean configLocked;
    private String sessionFactoryBeanName = "sessionFactory";
    private String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;

    protected static GrailsDomainBinder binder = new GrailsDomainBinder();

    /* (non-Javadoc)
     * @see org.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
     */
    public GrailsDomainConfiguration addDomainClass(GrailsDomainClass domainClass) {
        if (domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM)) {
            domainClasses.add(domainClass);
        }

        return this;
    }

    /* (non-Javadoc)
     * @see org.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setGrailsApplication(GrailsApplication application) {
        grailsApplication = application;
        if (grailsApplication == null) {
            return;
        }

        GrailsClass[] existingDomainClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        for (GrailsClass existingDomainClass : existingDomainClasses) {
            addDomainClass((GrailsDomainClass) existingDomainClass);
        }
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

    @Override
    public Settings buildSettings(ServiceRegistry serviceRegistry) {
        Settings settings = super.buildSettings(serviceRegistry);
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
    }

    @Override
    public Settings buildSettings(Properties props, ServiceRegistry serviceRegistry) throws HibernateException {
        Settings settings = super.buildSettings(props, serviceRegistry);
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
    }
    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        if (configLocked) {
            return;
        }

        // set the class loader to load Groovy classes
        if (grailsApplication != null) {
            Thread.currentThread().setContextClassLoader(grailsApplication.getClassLoader());
        }

        configureDomainBinder(grailsApplication, domainClasses);

        for (GrailsDomainClass domainClass : domainClasses) {
            if (!GrailsHibernateUtil.usesDatasource(domainClass, dataSourceName)) {
                continue;
            }
            final Mappings mappings = super.createMappings();
            Mapping m = binder.getMapping(domainClass);
            mappings.setAutoImport(m == null || m.getAutoImport());
            binder.bindClass(domainClass, mappings, sessionFactoryBeanName);
        }

        super.secondPassCompile();
        configLocked = true;
    }

    public static void configureDomainBinder(GrailsApplication grailsApplication, Set<GrailsDomainClass> domainClasses) {
        Closure defaultMapping = grailsApplication.getConfig().getProperty(DEFAULT_MAPPING, Closure.class);
        if(defaultMapping != null) {
            binder.setDefaultMapping(defaultMapping);
        }
        // do Grails class configuration
        for (GrailsDomainClass domainClass : domainClasses) {
            if (defaultMapping != null) {
                binder.evaluateMapping(domainClass, defaultMapping);
            }
            else {
                binder.evaluateMapping(domainClass);
            }
        }
    }
    
    @Override
    protected void reset() {
        super.reset();
        try {
            GrailsIdentifierGeneratorFactory.applyNewInstance(this);
        }
        catch (Exception e) {
            // ignore exception
        }
    }
}
