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
package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.util.Assert;

/**
 * A SessionFactory bean that allows the configuration class to
 * be changed and customise for usage within Grails.
 *
 * @author Graeme Rocher
 * @since 07-Jul-2005
 */
public class ConfigurableLocalSessionFactoryBean extends HibernateExceptionTranslator
          implements FactoryBean<SessionFactory>, ResourceLoaderAware, InitializingBean, DisposableBean,
                     ApplicationContextAware, BeanClassLoaderAware {

    private DataSource dataSource;
    private Resource[] configLocations;
    private String[] mappingResources;
    private Resource[] mappingLocations;
    private Resource[] cacheableMappingLocations;
    private Resource[] mappingJarLocations;
    private Resource[] mappingDirectoryLocations;
    private Interceptor entityInterceptor;
    private NamingStrategy namingStrategy;
    private Properties hibernateProperties;
    private Class<?>[] annotatedClasses;
    private String[] annotatedPackages;
    private String[] packagesToScan;
    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private GrailsAnnotationConfiguration configuration;
    private SessionFactory sessionFactory;

    private static final Log LOG = LogFactory.getLog(ConfigurableLocalSessionFactoryBean.class);
    protected GrailsApplication grailsApplication;
    protected ClassLoader classLoader;
    protected Class<?> configClass;
    protected Class<?> currentSessionContextClass;
    protected Map<String, Object> eventListeners;
    protected HibernateEventListeners hibernateEventListeners;
    protected ApplicationContext applicationContext;
    protected boolean proxyIfReloadEnabled = true;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;

    /**
     * Set the DataSource to be used by the SessionFactory.
     * If set, this will override corresponding settings in Hibernate properties.
     * <p>If this is set, the Hibernate settings should not define
     * a connection provider to avoid meaningless double configuration.
     * 
     * unwraps the dataSource if it's a TransactionAwareDataSourceProxy
     * Hibernate's Isolater.JdbcDelegate requires that the dataSource returns new connections for each getConnection call
     * 
     * @see org.hibernate.engine.transaction.Isolater.JdbcDelegate#delegateWork(org.hibernate.engine.transaction.IsolatedWork, boolean)
     * @see org.springframework.orm.hibernate3.AbstractSessionFactoryBean#setDataSource(javax.sql.DataSource)
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = (dataSource instanceof TransactionAwareDataSourceProxy) ? ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource() : dataSource;
    }
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the location of a single Hibernate XML config file, for example as
     * classpath resource "classpath:hibernate.cfg.xml".
     * <p>Note: Can be omitted when all necessary properties and mapping
     * resources are specified locally via this bean.
     * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
     */
    public void setConfigLocation(Resource configLocation) {
        configLocations = new Resource[] {configLocation};
    }

    /**
     * Set the locations of multiple Hibernate XML config files, for example as
     * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
     * <p>Note: Can be omitted when all necessary properties and mapping
     * resources are specified locally via this bean.
     * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
     */
    public void setConfigLocations(Resource[] configLocations) {
        this.configLocations = configLocations;
    }
    public Resource[] getConfigLocations() {
        return configLocations;
    }

    /**
     * Set Hibernate mapping resources to be found in the class path,
     * like "example.hbm.xml" or "mypackage/example.hbm.xml".
     * Analogous to mapping entries in a Hibernate XML config file.
     * Alternative to the more generic setMappingLocations method.
     * <p>Can be used to add to mappings from a Hibernate XML config file,
     * or to specify all mappings locally.
     * @see #setMappingLocations
     * @see org.hibernate.cfg.Configuration#addResource
     */
    public void setMappingResources(String[] mappingResources) {
        this.mappingResources = mappingResources;
    }
    public String[] getMappingResources() {
        return mappingResources;
    }

    /**
     * Set locations of Hibernate mapping files, for example as classpath
     * resource "classpath:example.hbm.xml". Supports any resource location
     * via Spring's resource abstraction, for example relative paths like
     * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
     * <p>Can be used to add to mappings from a Hibernate XML config file,
     * or to specify all mappings locally.
     * @see org.hibernate.cfg.Configuration#addInputStream
     */
    public void setMappingLocations(Resource[] mappingLocations) {
        this.mappingLocations = mappingLocations;
    }
    public Resource[] getMappingLocations() {
        return mappingLocations;
    }

    /**
     * Set locations of cacheable Hibernate mapping files, for example as web app
     * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
     * via Spring's resource abstraction, as long as the resource can be resolved
     * in the file system.
     * <p>Can be used to add to mappings from a Hibernate XML config file,
     * or to specify all mappings locally.
     * @see org.hibernate.cfg.Configuration#addCacheableFile(java.io.File)
     */
    public void setCacheableMappingLocations(Resource[] cacheableMappingLocations) {
        this.cacheableMappingLocations = cacheableMappingLocations;
    }
    public Resource[] getCacheableMappingLocations() {
        return cacheableMappingLocations;
    }

    /**
     * Set locations of jar files that contain Hibernate mapping resources,
     * like "WEB-INF/lib/example.hbm.jar".
     * <p>Can be used to add to mappings from a Hibernate XML config file,
     * or to specify all mappings locally.
     * @see org.hibernate.cfg.Configuration#addJar(java.io.File)
     */
    public void setMappingJarLocations(Resource[] mappingJarLocations) {
        this.mappingJarLocations = mappingJarLocations;
    }
    public Resource[] getMappingJarLocations() {
        return mappingJarLocations;
    }

    /**
     * Set locations of directories that contain Hibernate mapping resources,
     * like "WEB-INF/mappings".
     * <p>Can be used to add to mappings from a Hibernate XML config file,
     * or to specify all mappings locally.
     * @see org.hibernate.cfg.Configuration#addDirectory(java.io.File)
     */
    public void setMappingDirectoryLocations(Resource[] mappingDirectoryLocations) {
        this.mappingDirectoryLocations = mappingDirectoryLocations;
    }
    public Resource[] getMappingDirectoryLocations() {
        return mappingDirectoryLocations;
    }

    /**
     * Set a Hibernate entity interceptor that allows to inspect and change
     * property values before writing to and reading from the database.
     * Will get applied to any new Session created by this factory.
     * @see org.hibernate.cfg.Configuration#setInterceptor
     */
    public void setEntityInterceptor(Interceptor entityInterceptor) {
        this.entityInterceptor = entityInterceptor;
    }
    public Interceptor getEntityInterceptor() {
        return entityInterceptor;
    }

    /**
     * Set a Hibernate NamingStrategy for the SessionFactory, determining the
     * physical column and table names given the info in the mapping document.
     * @see org.hibernate.cfg.Configuration#setNamingStrategy
     */
    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    /**
     * Set Hibernate properties, such as "hibernate.dialect".
     * <p>Note: Do not specify a transaction provider here when using
     * Spring-driven transactions. It is also advisable to omit connection
     * provider settings and use a Spring-set DataSource instead.
     * @see #setDataSource
     */
    public void setHibernateProperties(Properties hibernateProperties) {
        this.hibernateProperties = hibernateProperties;
    }

    /**
     * Return the Hibernate properties, if any. Mainly available for
     * configuration through property paths that specify individual keys.
     */
    public Properties getHibernateProperties() {
        if (hibernateProperties == null) {
            hibernateProperties = new Properties();
        }
        return hibernateProperties;
    }

    /**
     * Specify annotated entity classes to register with this Hibernate SessionFactory.
     * @see org.hibernate.cfg.Configuration#addAnnotatedClass(Class)
     */
    public void setAnnotatedClasses(Class<?>[] annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    public Class<?>[] getAnnotatedClasses() {
        return annotatedClasses;
    }

    /**
     * Specify the names of annotated packages, for which package-level
     * annotation metadata will be read.
     * @see org.hibernate.cfg.Configuration#addPackage(String)
     */
    public void setAnnotatedPackages(String[] annotatedPackages) {
        this.annotatedPackages = annotatedPackages;
    }
    public String[] getAnnotatedPackages() {
        return annotatedPackages;
    }

    /**
     * Specify packages to search for autodetection of your entity classes in the
     * classpath. This is analogous to Spring's component-scan feature
     * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
     */
    public void setPackagesToScan(String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }
    public String[] getPackagesToScan() {
        return packagesToScan;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }

    /**
     * @param proxyIfReloadEnabled Sets whether a proxy should be created if reload is enabled
     */
    public void setProxyIfReloadEnabled(boolean proxyIfReloadEnabled) {
        this.proxyIfReloadEnabled = proxyIfReloadEnabled;
    }
    public boolean isProxyIfReloadEnabled() {
        return proxyIfReloadEnabled;
    }

    /**
     * Sets class to be used for the Hibernate CurrentSessionContext.
     *
     * @param currentSessionContextClass An implementation of the CurrentSessionContext interface
     */
    public void setCurrentSessionContextClass(Class<?> currentSessionContextClass) {
        this.currentSessionContextClass = currentSessionContextClass;
    }
    public Class<?> getCurrentSessionContextClass() {
        return currentSessionContextClass;
    }

    /**
     * Sets the class to be used for Hibernate Configuration.
     * @param configClass A subclass of the Hibernate Configuration class
     */
    public void setConfigClass(Class<?> configClass) {
        this.configClass = configClass;
    }
    public Class<?> getConfigClass() {
        return configClass;
    }

    /**
     * @param grailsApplication The grailsApplication to set.
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
    public GrailsApplication getGrailsApplication() {
        return grailsApplication;
    }

    public void setHibernateEventListeners(final HibernateEventListeners listeners) {
        hibernateEventListeners = listeners;
    }
    public HibernateEventListeners getHibernateEventListeners() {
        return hibernateEventListeners;
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }
    public String getSessionFactoryBeanName() {
        return sessionFactoryBeanName;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Specify the Hibernate event listeners to register, with listener types
     * as keys and listener objects as values. Instead of a single listener object,
     * you can also pass in a list or set of listeners objects as value.
     * <p>See the Hibernate documentation for further details on listener types
     * and associated listener interfaces.
     * @param eventListeners Map with listener type Strings as keys and
     * listener objects as values
     */
    public void setEventListeners(Map<String, Object> eventListeners) {
        this.eventListeners = eventListeners;
    }
    public Map<String, Object> getEventListeners() {
        return eventListeners;
    }

    public void afterPropertiesSet() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            buildSessionFactory();
        }
        finally {
            thread.setContextClassLoader(cl);
        }
    }

    protected void buildSessionFactory() throws Exception {

        configuration = newConfiguration();

        if (configLocations != null) {
            for (Resource resource : configLocations) {
                // Load Hibernate configuration from given location.
                configuration.configure(resource.getURL());
            }
        }

        if (mappingResources != null) {
            // Register given Hibernate mapping definitions, contained in resource files.
            for (String mapping : mappingResources) {
                Resource mr = new ClassPathResource(mapping.trim(), resourcePatternResolver.getClassLoader());
                configuration.addInputStream(mr.getInputStream());
            }
        }

        if (mappingLocations != null) {
            // Register given Hibernate mapping definitions, contained in resource files.
            for (Resource resource : mappingLocations) {
                configuration.addInputStream(resource.getInputStream());
            }
        }

        if (cacheableMappingLocations != null) {
            // Register given cacheable Hibernate mapping definitions, read from the file system.
            for (Resource resource : cacheableMappingLocations) {
                configuration.addCacheableFile(resource.getFile());
            }
        }

        if (mappingJarLocations != null) {
            // Register given Hibernate mapping definitions, contained in jar files.
            for (Resource resource : mappingJarLocations) {
                configuration.addJar(resource.getFile());
            }
        }

        if (mappingDirectoryLocations != null) {
            // Register all Hibernate mapping definitions in the given directories.
            for (Resource resource : mappingDirectoryLocations) {
                File file = resource.getFile();
                if (!file.isDirectory()) {
                    throw new IllegalArgumentException("Mapping directory location [" + resource + "] does not denote a directory");
                }
                configuration.addDirectory(file);
            }
        }

        if (entityInterceptor != null) {
            configuration.setInterceptor(entityInterceptor);
        }

        if (namingStrategy != null) {
            configuration.setNamingStrategy(namingStrategy);
        }

        if (hibernateProperties != null) {
            configuration.addProperties(hibernateProperties);
        }

        if (annotatedClasses != null) {
            configuration.addAnnotatedClasses(annotatedClasses);
        }

        if (annotatedPackages != null) {
            configuration.addPackages(annotatedPackages);
        }

        if (packagesToScan != null) {
            configuration.scanPackages(packagesToScan);
        }

        if (eventListeners != null) {
            configuration.setEventListeners(eventListeners);
         }

        sessionFactory = doBuildSessionFactory();

        buildSessionFactoryProxy();
    }

    protected SessionFactory doBuildSessionFactory() {
        return configuration.buildSessionFactory();
    }

    protected void buildSessionFactoryProxy() {
        try {
            if (!grails.util.Environment.getCurrent().isReloadEnabled() || !proxyIfReloadEnabled) {
                return;
            }

            // if reloading is enabled in this environment then we need to use a SessionFactoryProxy instance
            SessionFactoryProxy sfp = new SessionFactoryProxy();
            String suffix = dataSourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE) ? "" : '_' + dataSourceName;
            SessionFactoryHolder sessionFactoryHolder = applicationContext.getBean(
                    SessionFactoryHolder.BEAN_ID + suffix, SessionFactoryHolder.class);
            sessionFactoryHolder.setSessionFactory(sessionFactory);
            sfp.setApplicationContext(applicationContext);
            sfp.setCurrentSessionContextClass(currentSessionContextClass);
            sfp.setTargetBean(SessionFactoryHolder.BEAN_ID + suffix);
            sfp.afterPropertiesSet();
            sessionFactory = sfp;
        }
        catch (HibernateException e) {
            Throwable cause = e.getCause();
            if (isCacheConfigurationError(cause)) {
                LOG.error("There was an error configuring the Hibernate second level cache: " + getCauseMessage(e));
                LOG.error("This is normally due to one of two reasons. Either you have incorrectly specified the cache " +
                     "provider class name in [DataSource.groovy] or you do not have the cache provider on your classpath " +
                     "(eg. runtime (\"net.sf.ehcache:ehcache:1.6.1\"))");
                if (grails.util.Environment.isDevelopmentMode()) {
                    System.exit(1);
                }
            }
            throw e;
        }
    }

    /**
     * Return the Hibernate Configuration object used to build the SessionFactory.
     * Allows for access to configuration metadata stored there (rarely needed).
     * @throws IllegalStateException if the Configuration object has not been initialized yet
     */
    public final Configuration getConfiguration() {
        Assert.state(configuration != null, "Configuration not initialized yet");
        return configuration;
    }

    public SessionFactory getObject() {
        return sessionFactory;
    }

    public Class<?> getObjectType() {
        return sessionFactory == null ? SessionFactory.class : sessionFactory.getClass();
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() {
        if (grailsApplication.isWarDeployed()) {
            MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
            Map<?, ?> classMetaData = sessionFactory.getAllClassMetadata();
            for (Object o : classMetaData.values()) {
                registry.removeMetaClass(((ClassMetadata)o).getMappedClass());
            }
        }

        try {
            sessionFactory.close();
        }
        catch (HibernateException e) {
            if (e.getCause() instanceof NameNotFoundException) {
                LOG.debug(e.getCause().getMessage(), e);
            }
            else {
                throw e;
            }
        }
    }

    protected GrailsAnnotationConfiguration newConfiguration() throws Exception {
        if (configClass == null) {
            configClass = GrailsAnnotationConfiguration.class;
        }
        GrailsAnnotationConfiguration config = (GrailsAnnotationConfiguration) BeanUtils.instantiateClass(configClass);
        config.setApplicationContext(applicationContext);
        config.setGrailsApplication(grailsApplication);
        config.setSessionFactoryBeanName(sessionFactoryBeanName);
        config.setDataSourceName(dataSourceName);
        config.setHibernateEventListeners(hibernateEventListeners);
        if (currentSessionContextClass != null) {
            config.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, currentSessionContextClass.getName());
        }
        config.afterPropertiesSet();

        return config;
    }

    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        classLoader = beanClassLoader;
    }

    protected String getCauseMessage(HibernateException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause.getMessage();
    }

    protected boolean isCacheConfigurationError(Throwable cause) {
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause != null && (cause instanceof CacheException);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
