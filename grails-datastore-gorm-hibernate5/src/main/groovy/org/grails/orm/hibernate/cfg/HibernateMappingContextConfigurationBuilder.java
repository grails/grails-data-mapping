package org.grails.orm.hibernate.cfg;


import org.grails.orm.hibernate.EventListenerIntegrator;
import org.grails.orm.hibernate.GrailsSessionContext;
import org.grails.orm.hibernate.HibernateEventListeners;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentSessionContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.util.*;

public class HibernateMappingContextConfigurationBuilder
{
    private Class<? extends HibernateMappingContextConfiguration> configClass = HibernateMappingContextConfiguration.class;

    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;
    protected HibernateMappingContext hibernateMappingContext;
    private Class<? extends CurrentSessionContext> currentSessionContext = GrailsSessionContext.class;
    private HibernateEventListeners hibernateEventListeners;
    private Map<String, Object> eventListeners;
    private Properties properties = new Properties();


    public HibernateMappingContextConfigurationBuilder(Class<? extends HibernateMappingContextConfiguration> configClass)
    {
        this.configClass = configClass;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperty(String propertyName, String value) {
        properties.setProperty( propertyName, value );
    }

    public void setHibernateMappingContext(HibernateMappingContext hibernateMappingContext) {
        this.hibernateMappingContext = hibernateMappingContext;
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        String dsName = Mapping.DEFAULT_DATA_SOURCE.equals(dataSourceName) ? "dataSource" : "dataSource_" + dataSourceName;
        getProperties().put(Environment.DATASOURCE, applicationContext.getBean(dsName));
        getProperties().put(Environment.CURRENT_SESSION_CONTEXT_CLASS, currentSessionContext.getName());
        getProperties().put(AvailableSettings.CLASSLOADERS, applicationContext.getClassLoader());
    }

    public HibernateMappingContextConfiguration build()
    {

        Collection<ClassLoader> appClassLoaders = HibernateMappingContextConfiguration.getAppClassLoaders(getProperties(),getClass());

        final GrailsDomainBinder domainBinder = new GrailsDomainBinder(dataSourceName, sessionFactoryBeanName, hibernateMappingContext);

        ClassLoaderService classLoaderService = new ClassLoaderServiceImpl(appClassLoaders) {
            @Override
            public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
                if(MetadataContributor.class.isAssignableFrom(serviceContract)) {
                    return Collections.<S>singletonList((S) domainBinder);
                }
                else {
                    return super.loadJavaServices(serviceContract);
                }
            }
        };
        EventListenerIntegrator eventListenerIntegrator = new EventListenerIntegrator(hibernateEventListeners, eventListeners);
        BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
                .applyIntegrator(eventListenerIntegrator)
                .applyClassLoaderService(classLoaderService)
                .build();

        MetadataSources ms = new MetadataSources(bootstrapServiceRegistry);

        try
        {
            HibernateMappingContextConfiguration configuration = BeanUtils.instantiateClass(configClass.getConstructor(MetadataSources.class),ms);
            for(Object key : getProperties().keySet()) {
                configuration.getProperties().put(key,getProperties().get(key));
            }
            return configuration;

        } catch (NoSuchMethodException e)
        {
            throw new RuntimeException("There is no constructor in configClass which takes a single MetadataSources parameter.");
        }


    }


}
