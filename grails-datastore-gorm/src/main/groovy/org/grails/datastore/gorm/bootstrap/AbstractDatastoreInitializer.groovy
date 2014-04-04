package org.grails.datastore.gorm.bootstrap

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.compiler.gorm.GormTransformer
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ResourceLoaderAware
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
abstract class AbstractDatastoreInitializer implements ResourceLoaderAware{

    public static final String ENTITY_CLASS_RESOURCE_PATTERN = "/**/*.class"

    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver()
    Collection<Class> persistentClasses = []
    Collection<String> packages = []
    Properties configuration = new Properties()
    ConfigObject configurationObject = new ConfigObject()

    protected ClassLoader classLoader = Thread.currentThread().contextClassLoader

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

    AbstractDatastoreInitializer(Properties configuration, Collection<Class> persistentClasses) {
        this.configuration = configuration
        this.persistentClasses = persistentClasses
    }

    AbstractDatastoreInitializer(Properties configuration, Class... persistentClasses) {
        this(configuration, persistentClasses.toList())
    }

    @Override
    void setResourceLoader(ResourceLoader resourceLoader) {
        resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader)
    }

    protected void scanForPersistentClasses() {
        for (pkg in packages) {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(pkg) + ENTITY_CLASS_RESOURCE_PATTERN;

            def readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver)
            def resources = this.resourcePatternResolver.getResources(pattern)
            for (Resource res in resources) {
                def reader = readerFactory.getMetadataReader(res)
                if (reader.annotationMetadata.hasAnnotation("grails.persistence.Entity")) {
                    persistentClasses << classLoader.loadClass(reader.classMetadata.className)
                }
            }
        }
        def entityNames = GormTransformer.getKnownEntityNames()
        for (entityName in entityNames) {
            try {
                persistentClasses << classLoader.loadClass(entityName)
            } catch (ClassNotFoundException e) {
                // ignore
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
        this.configurationObject = new ConfigSlurper().parse(configuration)
        scanForPersistentClasses()

        ExpandoMetaClass.enableGlobally()

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

    Closure getCommonConfiguration(BeanDefinitionRegistry registry) {
        return {
            xmlns context: "http://www.springframework.org/schema/context"
            context.'annotation-config'()


            if(!registry.containsBeanDefinition(GrailsApplication.APPLICATION_ID)) {
                grailsApplication(DefaultGrailsApplication, persistentClasses as Class[], Thread.currentThread().contextClassLoader) { bean ->
                    bean.initMethod = 'initialise'
                }
            }

            for(cls in persistentClasses) {
                "${cls.name}"(cls) { bean ->
                    bean.singleton = false
                    bean.autowire = "byName"
                }
                "${cls.name}DomainClass"(MethodInvokingFactoryBean) { bean ->
                    targetObject = ref("grailsApplication")
                    targetMethod = "getArtefact"
                    bean.lazyInit = true
                    arguments = [DomainClassArtefactHandler.TYPE, cls.name]
                }
                "${cls.name}Validator"(GrailsDomainClassValidator) {
                    grailsApplication = ref("grailsApplication")
                    messageSource = ref("messageSource")
                    domainClass = ref("${cls.name}DomainClass")
                }
            }
        }
    }

    abstract public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)

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
