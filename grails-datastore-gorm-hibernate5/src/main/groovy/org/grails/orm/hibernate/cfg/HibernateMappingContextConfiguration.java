package org.grails.orm.hibernate.cfg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.IOException;
import java.util.*;

/**
 * A Configuration that uses a MappingContext to configure Hibernate
 *
 * @since 5.0
 */
public class HibernateMappingContextConfiguration extends Configuration implements ApplicationContextAware {
    private static final long serialVersionUID = -7115087342689305517L;

    protected static final Log LOG = LogFactory.getLog(HibernateMappingContextConfiguration.class);
    private static final String RESOURCE_PATTERN = "/**/*.class";

    private static final TypeFilter[] ENTITY_TYPE_FILTERS = new TypeFilter[] {
            new AnnotationTypeFilter(Entity.class, false),
            new AnnotationTypeFilter(Embeddable.class, false),
            new AnnotationTypeFilter(MappedSuperclass.class, false)};


    private ServiceRegistry serviceRegistry;
    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public HibernateMappingContextConfiguration(MetadataSources metadataSources)
    {
        super(metadataSources);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(applicationContext);
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
    public SessionFactory buildSessionFactory(ServiceRegistry registry) throws HibernateException
    {
        // set the class loader to load Groovy classes

        // work around for HHH-2624
        SessionFactory sessionFactory = null;


        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
        boolean overrideClassLoader = false;
        ClassLoader appClassLoader = null;
        Collection<ClassLoader> appClassLoaders = getAppClassLoaders(getProperties(),getClass());
        if(!appClassLoaders.isEmpty()) {
            for (ClassLoader cl : appClassLoaders) {
                if(!cl.equals(threadContextClassLoader)) {
                    overrideClassLoader = true;
                    appClassLoader = cl;
                    break;
                }
            }
        }
        if (overrideClassLoader) {
            currentThread.setContextClassLoader(appClassLoader);
        }

        try {
            ConfigurationHelper.resolvePlaceHolders(getProperties());



            setSessionFactoryObserver(new SessionFactoryObserver() {
                private static final long serialVersionUID = 1;
                public void sessionFactoryCreated(SessionFactory factory) {}
                public void sessionFactoryClosed(SessionFactory factory) {
                    ((ServiceRegistryImplementor)serviceRegistry).destroy();
                }
            });



            sessionFactory = super.buildSessionFactory(registry);
            serviceRegistry = ((SessionFactoryImplementor)sessionFactory).getServiceRegistry();
        }
        finally {
            if (overrideClassLoader) {
                currentThread.setContextClassLoader(threadContextClassLoader);
            }
        }

        return sessionFactory;
    }





    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
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

    public static Collection<ClassLoader> getAppClassLoaders(Properties properties, Class callerClass) {
        Object classLoaderObject = properties.get(AvailableSettings.CLASSLOADERS);
        Collection<ClassLoader> appClassLoaders;

        if(classLoaderObject instanceof Collection) {
            appClassLoaders = (Collection<ClassLoader>) classLoaderObject;
        }
        else if(classLoaderObject instanceof ClassLoader) {
            appClassLoaders = Collections.<ClassLoader>singletonList((ClassLoader) classLoaderObject);
        }
        else {
            appClassLoaders = Collections.emptyList();
        }

        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();

        if (appClassLoaders.isEmpty())
            appClassLoaders = Arrays.asList(threadContextClassLoader, callerClass.getClassLoader());

        return appClassLoaders;
    }
}
