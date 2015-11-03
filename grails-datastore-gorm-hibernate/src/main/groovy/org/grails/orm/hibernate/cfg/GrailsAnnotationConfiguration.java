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

import grails.core.*;
import groovy.lang.Closure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.orm.hibernate.proxy.GroovyAwarePojoEntityTuplizer;
import org.grails.core.artefact.AnnotationDomainClassArtefactHandler;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.*;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import java.util.*;

/**
 * Allows configuring Grails' hibernate support to work in conjunction with Hibernate's annotation
 * support.
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsAnnotationConfiguration extends Configuration implements GrailsDomainConfiguration {

    private static final long serialVersionUID = -7115087342689305517L;

    protected static final Log LOG = LogFactory.getLog(GrailsAnnotationConfiguration.class);

    protected GrailsApplication grailsApplication;
    protected Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    protected boolean configLocked;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;
    protected GrailsDomainBinder binder = new GrailsDomainBinder();

    private boolean subclassForeignKeysCreated = false;

    /* (non-Javadoc)
     * @see org.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
     */
    public GrailsDomainConfiguration addDomainClass(GrailsDomainClass domainClass) {
        if (shouldMapWithGorm(domainClass)) {
            domainClasses.add(domainClass);
        }
        else {
            addAnnotatedClass(domainClass.getClazz());
        }

        return this;
    }

    protected boolean shouldMapWithGorm(GrailsDomainClass domainClass) {
        return !AnnotationDomainClassArtefactHandler.isJPADomainClass(domainClass.getClazz()) &&
                domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM);
    }

    /* (non-Javadoc)
     * @see org.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setGrailsApplication(GrailsApplication application) {
        grailsApplication = application;
        if (grailsApplication == null) {
            return;
        }

        configureNamingStrategy();
        GrailsClass[] existingDomainClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        for (GrailsClass existingDomainClass : existingDomainClasses) {
            addDomainClass((GrailsDomainClass) existingDomainClass);
        }

        ArtefactHandler handler = grailsApplication.getArtefactHandler(DomainClassArtefactHandler.TYPE);
        if (!(handler instanceof AnnotationDomainClassArtefactHandler)) {
            return;
        }

        Set<String> jpaDomainNames = ((AnnotationDomainClassArtefactHandler)handler).getJpaClassNames();
        if (jpaDomainNames == null) {
            return;
        }

        final ClassLoader loader = grailsApplication.getClassLoader();
        for (String jpaDomainName : jpaDomainNames) {
            try {
                addAnnotatedClass(loader.loadClass(jpaDomainName));
            }
            catch (ClassNotFoundException e) {
                // impossible condition
            }
        }
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

    /* (non-Javadoc)
     * @see org.hibernate.cfg.Configuration#buildSessionFactory()
     */
    @Override
    public SessionFactory buildSessionFactory() throws HibernateException {
        // set the class loader to load Groovy classes
        if (grailsApplication != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] Setting context class loader to Grails GroovyClassLoader");
            }
            Thread.currentThread().setContextClassLoader(grailsApplication.getClassLoader());
        }

        // work around for HHH-2624
        addFilterDefinition(new FilterDefinition("dynamicFilterEnabler", "1=1", Collections.emptyMap()));

        SessionFactory sessionFactory = super.buildSessionFactory();

        if (grailsApplication != null) {
            GrailsHibernateUtil.configureHibernateDomainClasses(
                    sessionFactory, sessionFactoryBeanName, grailsApplication);
        }

        return sessionFactory;
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

    public void configureDomainBinder(GrailsDomainBinder binder, GrailsApplication grailsApplication, Set<GrailsDomainClass> domainClasses) {
        GrailsHibernateUtil.setBinder(binder);
        Closure defaultMapping = grailsApplication.getConfig().getProperty(GrailsDomainConfiguration.DEFAULT_MAPPING, Closure.class);
        // do Grails class configuration
        if(defaultMapping != null) {
            binder.setDefaultMapping(defaultMapping);
        }
        for (GrailsDomainClass domainClass : domainClasses) {
            if (defaultMapping != null) {
                binder.evaluateMapping(domainClass, defaultMapping);
            }
            else {
                binder.evaluateMapping(domainClass);
            }
        }
    }

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalContextLoader = currentThread.getContextClassLoader();
        if (!configLocked) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] [" + domainClasses.size() +
                        "] Grails domain classes to bind to persistence runtime");
            }

            // do Grails class configuration
            // configure the static binder first
            configureDomainBinder(binder, grailsApplication, domainClasses);

            for (GrailsDomainClass domainClass : domainClasses) {

                final String fullClassName = domainClass.getFullName();

                String hibernateConfig = fullClassName.replace('.', '/') + ".hbm.xml";
                final ClassLoader loader = originalContextLoader;
                // don't configure Hibernate mapped classes
                if (loader.getResource(hibernateConfig) != null) continue;

                final Mappings mappings = super.createMappings();
                if (!GrailsHibernateUtil.usesDatasource(domainClass, dataSourceName)) {
                    continue;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsAnnotationConfiguration] Binding persistent class [" + fullClassName + "]");
                }

                Mapping m = binder.getMapping(domainClass);
                mappings.setAutoImport(m == null || m.getAutoImport());
                binder.bindClass(domainClass, mappings, sessionFactoryBeanName);
            }
        }

        try {
            currentThread.setContextClassLoader(grailsApplication.getClassLoader());
            super.secondPassCompile();
            createSubclassForeignKeys();

        } finally {
            currentThread.setContextClassLoader(originalContextLoader);
        }

        configLocked = true;
    }

    /**
     * Creates foreign keys for subclass tables that are mapped using table per subclass. Further information is
     * available in the <a href="http://jira.grails.org/browse/GRAILS-7729">JIRA ticket</a>
     */
    private void createSubclassForeignKeys() {
        if (subclassForeignKeysCreated) {
            return;
        }

        for (PersistentClass persistentClass : classes.values()) {
            if (persistentClass instanceof RootClass) {
                RootClass rootClass = (RootClass) persistentClass;

                if (rootClass.hasSubclasses()) {
                    Iterator subclasses = rootClass.getSubclassIterator();

                    while (subclasses.hasNext()) {

                        Object subclass = subclasses.next();

                        // This test ensures that foreign keys will only be created for subclasses that are
                        // mapped using "table per subclass"
                        if (subclass instanceof JoinedSubclass) {
                            JoinedSubclass joinedSubclass = (JoinedSubclass) subclass;
                            joinedSubclass.createForeignKey();
                        }
                    }
                }
            }
        }

        subclassForeignKeysCreated = true;
    }

    /**
     * Sets custom naming strategy specified in configuration or the default {@link ImprovedNamingStrategy}.
     */
    protected void configureNamingStrategy() {
        NamingStrategy strategy = null;
        Object customStrategy = grailsApplication.getFlatConfig().get("hibernate.naming_strategy");
        if (customStrategy != null) {
            Class<?> namingStrategyClass = null;
            if (customStrategy instanceof Class<?>) {
                namingStrategyClass = (Class<?>)customStrategy;
            } else {
                try {
                    namingStrategyClass = grailsApplication.getClassLoader().loadClass(customStrategy.toString());
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

            if (namingStrategyClass != null) {
                try {
                    strategy = (NamingStrategy)namingStrategyClass.newInstance();
                } catch (InstantiationException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }

        if (strategy == null) {
            strategy = ImprovedNamingStrategy.INSTANCE;
        }

        setNamingStrategy(strategy);
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
