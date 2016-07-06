package org.grails.orm.hibernate.connections

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.orm.hibernate.AbstractEventTriggeringInterceptor
import org.grails.orm.hibernate.HibernateEventListeners
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings
import org.grails.orm.hibernate.support.AbstractClosureEventTriggeringInterceptor
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.ImprovedNamingStrategy
import org.hibernate.cfg.NamingStrategy
import org.springframework.core.io.Resource

/**
 * Settings for Hibernate
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
@AutoClone
class HibernateConnectionSourceSettings extends ConnectionSourceSettings {

    /**
     * Whether to prepare the datastore for runtime reloading
     */
    boolean enableReload = false
    /**
     * Settings for the dataSource
     */
    DataSourceSettings dataSource = new DataSourceSettings()

    /**
     * Settings for Hibernate
     */
    HibernateSettings hibernate = new HibernateSettings()

    /**
     * Convert to hibernate properties
     *
     * @return The properties
     */
    Properties toProperties() {
        Properties properties = new Properties()
        properties.putAll( dataSource.toHibernateProperties() )
        properties.putAll( hibernate.toProperties() )

        return properties
    }


    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    @AutoClone
    static class HibernateSettings extends LinkedHashMap<String, String> {

        /**
         * Whether OpenSessionInView should be read-only
         */
        OsivSettings osiv = new OsivSettings()
        /**
         * Whether Hibernate should be in read-only mode
         */
        boolean readOnly = false
        /**
         * Cache settings
         */
        CacheSettings cache = new CacheSettings()

        /**
         * Flush settings
         */
        FlushSettings flush = new FlushSettings()


        /**
         * The configuration class
         */
        Class<? extends Configuration> configClass

        /**
         * The naming strategy
         */
        Class<? extends NamingStrategy> naming_strategy = ImprovedNamingStrategy

        /**
         * A subclass of AbstractClosureEventTriggeringInterceptor
         */
        Class<? extends AbstractClosureEventTriggeringInterceptor> closureEventTriggeringInterceptorClass

        /**
         * The event triggering interceptor
         */
        AbstractClosureEventTriggeringInterceptor eventTriggeringInterceptor
        /**
         * The default hibernate event listeners
         */
        HibernateEventListeners hibernateEventListeners = new HibernateEventListeners()

        /**
         * Set the locations of multiple Hibernate XML config files, for example as
         * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
         * <p>Note: Can be omitted when all necessary properties and mapping
         * resources are specified locally via this bean.
         * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
         */

        Resource[] configLocations

        /**
         * Set locations of Hibernate mapping files, for example as classpath
         * resource "classpath:example.hbm.xml". Supports any resource location
         * via Spring's resource abstraction, for example relative paths like
         * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
         * <p>Can be used to add to mappings from a Hibernate XML config file,
         * or to specify all mappings locally.
         * @see org.hibernate.cfg.Configuration#addInputStream
         */
        Resource[] mappingLocations

        /**
         * Set locations of cacheable Hibernate mapping files, for example as web app
         * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
         * via Spring's resource abstraction, as long as the resource can be resolved
         * in the file system.
         * <p>Can be used to add to mappings from a Hibernate XML config file,
         * or to specify all mappings locally.
         * @see org.hibernate.cfg.Configuration#addCacheableFile(java.io.File)
         */
        Resource[] cacheableMappingLocations

        /**
         * Set locations of jar files that contain Hibernate mapping resources,
         * like "WEB-INF/lib/example.hbm.jar".
         * <p>Can be used to add to mappings from a Hibernate XML config file,
         * or to specify all mappings locally.
         * @see org.hibernate.cfg.Configuration#addJar(java.io.File)
         */
        Resource[] mappingJarLocations

        /**
         * Set locations of directories that contain Hibernate mapping resources,
         * like "WEB-INF/mappings".
         * <p>Can be used to add to mappings from a Hibernate XML config file,
         * or to specify all mappings locally.
         * @see org.hibernate.cfg.Configuration#addDirectory(java.io.File)
         */
        Resource[] mappingDirectoryLocations

        /**
         * Specify annotated entity classes to register with this Hibernate SessionFactory.
         * @see org.hibernate.cfg.Configuration#addAnnotatedClass(Class)
         */
        Class<?>[] annotatedClasses

        /**
         * Specify the names of annotated packages, for which package-level
         * annotation metadata will be read.
         * @see org.hibernate.cfg.Configuration#addPackage(String)
         */
        String[] annotatedPackages

        /**
         * Specify packages to search for autodetection of your entity classes in the
         * classpath. This is analogous to Spring's component-scan feature
         * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
         */
        String[] packagesToScan



        @CompileStatic
        Map<String, Object> toHibernateEventListeners(AbstractClosureEventTriggeringInterceptor eventTriggeringInterceptor) {
            if(eventTriggeringInterceptor != null) {
                return [
                    'save': eventTriggeringInterceptor,
                    'save-update': eventTriggeringInterceptor,
                    'pre-load': eventTriggeringInterceptor,
                    'post-load': eventTriggeringInterceptor,
                    'pre-insert': eventTriggeringInterceptor,
                    'post-insert': eventTriggeringInterceptor,
                    'pre-update': eventTriggeringInterceptor,
                    'post-update': eventTriggeringInterceptor,
                    'pre-delete': eventTriggeringInterceptor,
                    'post-delete': eventTriggeringInterceptor
                ] as Map<String,Object>
            }
            return Collections.emptyMap()
        }

        /**
         * Convert to Hibernate properties
         *
         * @return The hibernate properties
         */
        @CompileStatic
        Properties toProperties() {
            Properties props = new Properties()
            for(String key in keySet()) {
                props.put("hibernate.$key".toString(), get(key))
            }
            if(naming_strategy != null) {
                props.put("hibernate.naming_strategy".toString(), naming_strategy.name)
            }
            if(configClass != null) {
                props.put("hibernate.config_class".toString(), configClass.name)
            }
            return props
        }

        @Builder(builderStrategy = SimpleStrategy, prefix = '')
        @AutoClone
        static class CacheSettings {
            /**
             * Whether to cache queries
             */
            boolean queries = false
        }

        @Builder(builderStrategy = SimpleStrategy, prefix = '')
        @AutoClone
        static class FlushSettings {
            /**
             * The default flush mode
             */
            FlushMode mode = FlushMode.AUTO

            /**
             * We use a separate enum here because the classes differ between Hibernate 3 and 4
             *
             * @see org.hibernate.FlushMode
             */
            static enum FlushMode {
                MANUAL(0),
                COMMIT(5),
                AUTO(10),
                ALWAYS(20);

                private final int level;

                FlushMode(int level) {
                    this.level = level;
                }

                public int getLevel() {
                    return level;
                }
            }
        }

        /**
         * Settings for OpenSessionInView
         */
        @Builder(builderStrategy = SimpleStrategy, prefix = '')
        @AutoClone
        static class OsivSettings {
            /**
             * Whether to cache queries
             */
            boolean readonly = false

            /**
             * Whether OSIV is enabled
             */
            boolean enabled = true
        }


    }
}
