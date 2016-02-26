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
import groovy.lang.Closure;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.orm.hibernate.datasource.MultipleDataSourceSupport;
import org.grails.orm.hibernate.proxy.GroovyAwarePojoEntityTuplizer;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.Settings;

/**
 * Creates runtime configuration mapping for the Grails domain classes
 * based on the work done in the Hibernate Annotations project
 *
 * @author Graeme Rocher
 * @since 06-Jul-2005
 */
public class DefaultGrailsDomainConfiguration extends Configuration implements GrailsDomainConfiguration {

    private static final long serialVersionUID = -7115087342689305517L;

    protected GrailsApplication grailsApplication;
    protected Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    protected boolean configLocked;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;

    protected GrailsDomainBinder binder = new GrailsDomainBinder();

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

        for (GrailsClass existingDomainClass : grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)) {
            addDomainClass((GrailsDomainClass) existingDomainClass);
        }
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        if (configLocked) {
            return;
        }

        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        try {
            // set the class loader to load Groovy classes
            if (grailsApplication != null) {
                thread.setContextClassLoader(grailsApplication.getClassLoader());
            }

            // set the class loader to load Groovy classes
            final Mappings mappings = super.createMappings();
            Closure defaultMapping = grailsApplication.getConfig().getProperty(GrailsDomainConfiguration.DEFAULT_MAPPING, Closure.class);
            MappingContext mappingContext = new HibernateMappingContext(defaultMapping);

            for (GrailsDomainClass domainClass : domainClasses) {
                final PersistentEntity entity = mappingContext.addPersistentEntity(domainClass.getClazz());
                if (entity != null) {
                    mappingContext.addEntityValidator(entity, domainClass.getValidator());
                    binder.evaluateMapping(entity);
                }
            }

            for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
                if (!MultipleDataSourceSupport.usesDatasource(entity, dataSourceName)) {
                    continue;
                }
                Mapping m = binder.getMapping(entity);
                mappings.setAutoImport(m == null || m.getAutoImport());
                binder.bindClass(entity, mappings, sessionFactoryBeanName);
            }

            super.secondPassCompile();
            configLocked = true;
        }
        finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }



    @Override
    public Settings buildSettings() {
        Settings settings = super.buildSettings();
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
    }

    @Override
    public Settings buildSettings(Properties props) throws HibernateException {
        Settings settings = super.buildSettings(props);
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
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
