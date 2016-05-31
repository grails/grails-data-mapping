package org.grails.datastore.gorm.bootstrap

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.plugin.support.PersistenceContextInterceptorAggregator
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar
import org.grails.datastore.mapping.reflect.AstUtils
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.util.ClassUtils

/**
 * Abstract class for datastore initializers to implement
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */
@CompileStatic
abstract class AbstractDatastoreInitializer implements ResourceLoaderAware{

    public static final String TRANSACTION_MANAGER_BEAN = 'transactionManager'
    public static final String ENTITY_CLASS_RESOURCE_PATTERN = "/**/*.class"
    public static final String OSIV_CLASS_NAME = 'org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor'


    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver()
    Collection<Class> persistentClasses = []
    Collection<String> packages = []
    PropertyResolver configuration = new StandardEnvironment()
    boolean registerApplicationIfNotPresent = true

    protected ClassLoader classLoader = Thread.currentThread().contextClassLoader
    protected boolean secondaryDatastore = false

    AbstractDatastoreInitializer() {
    }

    AbstractDatastoreInitializer(ClassLoader classLoader, String... packages) {
        this(packages)
        this.classLoader = classLoader ?: Thread.currentThread().contextClassLoader
    }
    AbstractDatastoreInitializer(String... packages) {
        this.packages = packages.toList()
    }

    AbstractDatastoreInitializer(Collection<Class> persistentClasses) {
        this.persistentClasses = persistentClasses
    }

    AbstractDatastoreInitializer(Class... persistentClasses) {
        this(persistentClasses.toList())
    }

    AbstractDatastoreInitializer(PropertyResolver configuration, Collection<Class> persistentClasses) {
        this.configuration = configuration
        this.persistentClasses = persistentClasses
    }

    AbstractDatastoreInitializer(PropertyResolver configuration, Class...persistentClasses) {
        this.configuration = configuration
        this.persistentClasses = Arrays.asList(persistentClasses)
    }

    AbstractDatastoreInitializer(PropertyResolver configuration, String...packages) {
        this.configuration = configuration
        this.packages = Arrays.asList(packages)
    }

    AbstractDatastoreInitializer(Map configuration, Collection<Class> persistentClasses) {
        // Grails 3.x has better support for property resolving from configuration so we should
        // if the config is a PropertyResolver
        if(configuration instanceof PropertyResolver) {
            this.configuration = (PropertyResolver)configuration;
        }
        else {
            // this is to support Grails 2.x
            def env = new StandardEnvironment()
            def config = new ConfigObject()
            if(configuration instanceof ConfigObject) {
                config = (ConfigObject) configuration
            }
            else if(configuration != null) {
                def properties = new Properties()
                properties.putAll(configuration)
                config.merge(new ConfigSlurper().parse(properties))
                config.putAll( config.flatten() )
            }
            env.propertySources.addFirst(new MapPropertySource("datastoreConfig", config) {
                @Override
                boolean containsProperty(String name) {
                    getProperty(name) != null
                }

                @Override
                Object getProperty(String name) {
                    def v = super.getProperty(name)
                    if(v != null) {
                        return v
                    } else if(name.contains('.')) {
                        def map = getSource()
                        def tokens = name.split(/\./)
                        int i = 0
                        v = map.get(tokens[i])
                        while(v instanceof Map && i < tokens.length -1) {
                            Map co = (Map)v
                            v = co.get(tokens[++i])
                        }

                        return v

                    }
                }
            })
            this.configuration = env
        }
        this.persistentClasses = persistentClasses
    }

    AbstractDatastoreInitializer(Map configuration, Class... persistentClasses) {
        this(configuration, persistentClasses.toList())
    }

    /**
     * Configures for an existing Mongo instance
     * @param mongo The instance of Mongo
     * @return The configured ApplicationContext
     */
    @CompileStatic
    ApplicationContext configure() {
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }
    /**
     * Whether this datastore is secondary to another primary datastore (example the SQL DB)
     *
     * @param secondaryDatastore
     */
    void setSecondaryDatastore(boolean secondaryDatastore) {
        this.secondaryDatastore = secondaryDatastore
    }

    @Override
    void setResourceLoader(ResourceLoader resourceLoader) {
        resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader)
    }

    @CompileStatic
    protected void scanForPersistentClasses() {
        // scan defined packages
        def readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver)
        for (pkg in packages) {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(pkg) + ENTITY_CLASS_RESOURCE_PATTERN;

            scanUsingPattern(pattern, readerFactory)
        }


        def entityNames = AstUtils.getKnownEntityNames()
        if(entityNames) {
            // only works at development time
            for (entityName in entityNames) {
                try {
                    persistentClasses << classLoader.loadClass(entityName)
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
        else {
            // try the default package in case of a script without recursing into subpackages
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +  "*.class"
            scanUsingPattern(pattern, readerFactory)
        }
    }

    @CompileStatic
    private void scanUsingPattern(String pattern, CachingMetadataReaderFactory readerFactory) {
        def resources = this.resourcePatternResolver.getResources(pattern)
        for (Resource res in resources) {
            def reader = readerFactory.getMetadataReader(res)
            if (reader.annotationMetadata.hasAnnotation("grails.persistence.Entity")) {
                persistentClasses << classLoader.loadClass(reader.classMetadata.className)
            }
        }
    }

    /**
     * Configures an existing BeanDefinitionRegistry
     *
     * @param beanDefinitionRegistry The BeanDefinitionRegistry to configure
     */
    @CompileStatic
    void configureForBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {

        if(configuration instanceof ConfigurableEnvironment && beanDefinitionRegistry instanceof ConfigurableApplicationContext) {
            def env = (ConfigurableEnvironment) configuration

            def conversionService = ((ConfigurableApplicationContext) beanDefinitionRegistry).getEnvironment().getConversionService()

            BasicTypeConverterRegistrar registrar = new BasicTypeConverterRegistrar()
            registrar.register(conversionService)
            env.setConversionService(conversionService)
        }

        scanForPersistentClasses()

        if( GroovyBeanReaderInit.isAvailable() ) {
            GroovyBeanReaderInit.registerBeans(beanDefinitionRegistry, getBeanDefinitions(beanDefinitionRegistry))
        }
        else if (GrailsBeanBuilderInit.isAvailable() ) {
            GrailsBeanBuilderInit.registerBeans(beanDefinitionRegistry, getBeanDefinitions(beanDefinitionRegistry))
        }
        else {
            throw new IllegalStateException("Neither Spring 4.0 nor grails-spring dependency found on classpath to enable GORM configuration. If you are using an earlier version of Spring please add the grails-spring dependency to your classpath.")
        }
    }

    @CompileDynamic
    Closure getCommonConfiguration(BeanDefinitionRegistry registry, String type) {
        return {
            if(!registry.containsBeanDefinition("grailsApplication") && registerApplicationIfNotPresent) {
                grailsApplication(getGrailsApplicationClass(), persistentClasses as Class[], Thread.currentThread().contextClassLoader) { bean ->
                    bean.initMethod = 'initialise'
                }
            }

            Collection<Class> classes = collectMappedClasses(type)
            for(cls in classes) {
                "${cls.name}"(cls) { bean ->
                    bean.singleton = false
                    bean.autowire = "byName"
                }
                "${cls.name}DomainClass"(MethodInvokingFactoryBean) { bean ->
                    targetObject = ref("grailsApplication")
                    targetMethod = "getArtefact"
                    bean.lazyInit = true
                    arguments = [AstUtils.DOMAIN_TYPE, cls.name]
                }
                "${cls.name}Validator"(getGrailsValidatorClass()) {
                    grailsApplication = ref("grailsApplication")
                    datastoreName = "${type}Datastore"
                    messageSource = ref("messageSource")
                    domainClass = ref("${cls.name}DomainClass")
                }
            }
        }
    }


    protected Collection<Class> collectMappedClasses(String datastoreType) {
        def classes = !secondaryDatastore ? persistentClasses : persistentClasses.findAll() { Class cls ->
            isMappedClass(datastoreType, cls)
        }
        return classes
    }

    protected boolean isMappedClass(String datastoreType, Class cls) {
        datastoreType.equals(ClassPropertyFetcher.forClass(cls).getStaticPropertyValue(GormProperties.MAPPING_STRATEGY, cls))
    }


    abstract public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)

    /**
     * Internal method aiding in datastore configuration.
     *
     * @param registry The BeanDefinitionRegistry
     * @param type The type of the datastore
     *
     * @return A closure containing bean definitions
     */
    @CompileDynamic
    Closure getAdditionalBeansConfiguration(BeanDefinitionRegistry registry, String type) {
        {->
            "${type}TransactionManager"(DatastoreTransactionManager) {
                datastore = ref("${type}Datastore")
            }


            def transactionManagerBeanName = TRANSACTION_MANAGER_BEAN
            if (!containsRegisteredBean(delegate, registry, transactionManagerBeanName)) {
                registry.registerAlias("${type}TransactionManager", transactionManagerBeanName)
            }

            "${type}PersistenceInterceptor"(getPersistenceInterceptorClass(), ref("${type}Datastore"))

            "${type}PersistenceContextInterceptorAggregator"(PersistenceContextInterceptorAggregator)


            def classLoader = Thread.currentThread().contextClassLoader
            if (registry.containsBeanDefinition('dispatcherServlet') && ClassUtils.isPresent(OSIV_CLASS_NAME, classLoader)) {
                String interceptorName = "${type}OpenSessionInViewInterceptor"
                "${interceptorName}"(ClassUtils.forName(OSIV_CLASS_NAME, classLoader)) {
                    datastore = ref("${type}Datastore")
                }
            }
        }
    }

    @CompileDynamic
    protected boolean containsRegisteredBean(Object builder, BeanDefinitionRegistry registry, String beanName) {
        registry.containsBeanDefinition(beanName) || (builder.hasProperty('springConfig') && builder.springConfig.containsBean(beanName))
    }

    /**
     * @return The class used to define the persistence interceptor
     */
    protected abstract Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass()

    @CompileStatic
    protected Class getGrailsApplicationClass() {
        ClassLoader cl = Thread.currentThread().contextClassLoader
        if(ClassUtils.isPresent("grails.core.DefaultGrailsApplication", cl)) {
            return ClassUtils.forName("grails.core.DefaultGrailsApplication", cl)
        }
        if(ClassUtils.isPresent("org.codehaus.groovy.grails.commons.DefaultGrailsApplication", cl)) {
            return ClassUtils.forName("org.codehaus.groovy.grails.commons.DefaultGrailsApplication", cl)
        }
        throw new IllegalStateException("No version of Grails found on classpath")

    }

    @CompileStatic
    protected Class getGrailsValidatorClass() {
        ClassLoader cl = Thread.currentThread().contextClassLoader
        if(ClassUtils.isPresent("org.grails.datastore.gorm.validation.DefaultDomainClassValidator", cl)) {
            return ClassUtils.forName("org.grails.datastore.gorm.validation.DefaultDomainClassValidator", cl)
        }
        throw new IllegalStateException("No version of Grails found on classpath")
    }

    @CompileDynamic
    static class GroovyBeanReaderInit {
        static boolean isAvailable() {
            try {
                Thread.currentThread().contextClassLoader.loadClass('org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader')
                return true
            } catch (e) {
                return false
            }
        }
        static void registerBeans(BeanDefinitionRegistry registry, Closure beanDefinitions) {
            def classLoader = Thread.currentThread().contextClassLoader
            def beanReader = classLoader.loadClass('org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader').newInstance(registry)
            beanReader.beans beanDefinitions
        }
    }

    @CompileDynamic
    static class GrailsBeanBuilderInit {
        static boolean isAvailable() {
            try {
                Thread.currentThread().contextClassLoader.loadClass('grails.spring.BeanBuilder')
                return true
            } catch (e) {
                return false
            }
        }

        static void registerBeans(BeanDefinitionRegistry registry, Closure beanDefinitions) {
            def classLoader = Thread.currentThread().contextClassLoader
            def beanBuilder = classLoader.loadClass('grails.spring.BeanBuilder').newInstance()
            beanBuilder.beans beanDefinitions
            beanBuilder.registerBeans registry
        }
    }
}
