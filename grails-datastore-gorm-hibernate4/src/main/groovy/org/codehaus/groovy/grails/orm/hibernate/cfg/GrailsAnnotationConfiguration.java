/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.EventListenerIntegrator;
import org.codehaus.groovy.grails.orm.hibernate.GrailsSessionContext;
import org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Allows configuring Grails' hibernate support to work in conjuntion with Hibernate's annotation
 * support.
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsAnnotationConfiguration extends Configuration implements GrailsDomainConfiguration, InitializingBean {

    private static final long serialVersionUID = -7115087342689305517L;

    protected Logger log = LoggerFactory.getLogger(getClass());

    private GrailsApplication grailsApplication;
    private Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    private boolean configLocked;
    private String sessionFactoryBeanName = "sessionFactory";
    private String dataSourceName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;

    private static final String RESOURCE_PATTERN = "/**/*.class";

    private static final TypeFilter[] ENTITY_TYPE_FILTERS = new TypeFilter[] {
          new AnnotationTypeFilter(Entity.class, false),
          new AnnotationTypeFilter(Embeddable.class, false),
          new AnnotationTypeFilter(MappedSuperclass.class, false)};

    private ResourcePatternResolver resourcePatternResolver;
    private ServiceRegistry serviceRegistry;
    private HibernateEventListeners hibernateEventListeners;
    private Map<String, Object> eventListeners;

    protected GrailsDomainBinder binder = new GrailsDomainBinder();

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
     */
    public GrailsDomainConfiguration addDomainClass(GrailsDomainClass domainClass) {
        if (shouldMapWithGorm(domainClass)) {
            domainClasses.add(domainClass);
        }

        return this;
    }

    private boolean shouldMapWithGorm(GrailsDomainClass domainClass) {
        return !AnnotationDomainClassArtefactHandler.isJPADomainClass(domainClass.getClazz()) &&
               domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
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

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalContextLoader = currentThread.getContextClassLoader();
        if (!configLocked) {
            log.debug("[GrailsAnnotationConfiguration] [{}] Grails domain classes to bind to persistence runtime", domainClasses.size());

            // do Grails class configuration
            DefaultGrailsDomainConfiguration.configureDomainBinder(grailsApplication, domainClasses);

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

                log.debug("[GrailsAnnotationConfiguration] Binding persistent class [{}]", fullClassName);

                Mapping m = binder.getMapping(domainClass);
                mappings.setAutoImport(m == null || m.getAutoImport());
                binder.bindClass(domainClass, mappings, sessionFactoryBeanName);
            }
        }

        try {
            currentThread.setContextClassLoader(grailsApplication.getClassLoader());
            super.secondPassCompile();
        } finally {
            currentThread.setContextClassLoader(originalContextLoader);
        }

        configLocked = true;
    }

    /**
     * Sets custom naming strategy specified in configuration or the default {@link ImprovedNamingStrategy}.
     */
    private void configureNamingStrategy() {
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

    /**
     * Add the given annotated classes in a batch.
     * @see #addAnnotatedClass
     * @see #scanPackages
     */
    public void addAnnotatedClasses(Class<?>... annotatedClasses) {
        for (Class<?> annotatedClass : annotatedClasses) {
            addAnnotatedClass(annotatedClass);
        }
    }

    /**
     * Add the given annotated packages in a batch.
     * @see #addPackage
     * @see #scanPackages
     */
    public void addPackages(String... annotatedPackages) {
        for (String annotatedPackage :annotatedPackages) {
            addPackage(annotatedPackage);
        }
    }

    /**
     * Default listeners.
     * @param listeners the listeners
     */
    public void setEventListeners(Map<String, Object> listeners) {
        eventListeners = listeners;
    }

    /**
     * User-specifiable extra listeners.
     * @param listeners the listeners
     */
    public void setHibernateEventListeners(HibernateEventListeners listeners) {
        hibernateEventListeners = listeners;
    }

    /**
     * Perform Spring-based scanning for entity classes, registering them
     * as annotated classes with this {@code Configuration}.
     * @param packagesToScan one or more Java package names
     * @throws HibernateException if scanning fails for any reason
     */
    public void scanPackages(String... packagesToScan) throws HibernateException {
        try {
            for (String pkg : packagesToScan) {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
                Resource[] resources = resourcePatternResolver.getResources(pattern);
                MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        if (matchesFilter(reader, readerFactory)) {
                            addAnnotatedClasses(resourcePatternResolver.getClassLoader().loadClass(className));
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            throw new MappingException("Failed to scan classpath for unlisted classes", ex);
        }
        catch (ClassNotFoundException ex) {
            throw new MappingException("Failed to load annotated classes from classpath", ex);
        }
    }

    /**
     * Check whether any of the configured entity type filters matches
     * the current class descriptor contained in the metadata reader.
     */
    protected boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
        for (TypeFilter filter : ENTITY_TYPE_FILTERS) {
            if (filter.match(reader, readerFactory)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SessionFactory buildSessionFactory() throws HibernateException {

        // set the class loader to load Groovy classes
        if (grailsApplication != null) {
            log.debug("[GrailsAnnotationConfiguration] Setting context class loader to Grails GroovyClassLoader");
            Thread.currentThread().setContextClassLoader(grailsApplication.getClassLoader());
        }

        // work around for HHH-2624
        Map<String, Type> empty = new HashMap<String, Type>();
        addFilterDefinition(new FilterDefinition("dynamicFilterEnabler", "1=1", empty));

        SessionFactory sessionFactory = null;

        ClassLoader appClassLoader = (ClassLoader) getProperties().get(AvailableSettings.APP_CLASSLOADER);
        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
        boolean overrideClassLoader = (appClassLoader != null && !appClassLoader.equals(threadContextClassLoader));
        if (overrideClassLoader) {
            currentThread.setContextClassLoader(appClassLoader);
        }

        try {
            ConfigurationHelper.resolvePlaceHolders(getProperties());

            EventListenerIntegrator eventListenerIntegrator = new EventListenerIntegrator(hibernateEventListeners, eventListeners);
            BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().with(eventListenerIntegrator).build();

            setSessionFactoryObserver(new SessionFactoryObserver() {
                private static final long serialVersionUID = 1;
                public void sessionFactoryCreated(SessionFactory factory) {}
                public void sessionFactoryClosed(SessionFactory factory) {
                    ((ServiceRegistryImplementor)serviceRegistry).destroy();
                }
            });

            sessionFactory = super.buildSessionFactory(new ServiceRegistryBuilder(bootstrapServiceRegistry).applySettings(getProperties()).buildServiceRegistry());
            serviceRegistry = ((SessionFactoryImplementor)sessionFactory).getServiceRegistry();
        }
        finally {
            if (overrideClassLoader) {
                currentThread.setContextClassLoader(threadContextClassLoader);
            }
        }

        if (grailsApplication != null) {
            GrailsHibernateUtil.configureHibernateDomainClasses(
                    sessionFactory, sessionFactoryBeanName, grailsApplication);
        }

        return sessionFactory;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void afterPropertiesSet() throws Exception {
        if (grailsApplication == null) {
            return;
        }

        String dsName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE.equals(dataSourceName) ? "dataSource" : "dataSource_" + dataSourceName;
        getProperties().put(Environment.DATASOURCE, grailsApplication.getMainContext().getBean(dsName));
        getProperties().put(Environment.CURRENT_SESSION_CONTEXT_CLASS, GrailsSessionContext.class.getName());
        getProperties().put(AvailableSettings.APP_CLASSLOADER, grailsApplication.getClassLoader());
        resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(grailsApplication.getMainContext());

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
}
