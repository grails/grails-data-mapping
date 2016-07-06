package org.grails.orm.hibernate.connections

import groovy.transform.AutoClone
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.NamingStrategy

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
        Class<? extends NamingStrategy> naming_strategy

        /**
         * Any additional event listeners
         */
        Map<String, Object> eventListeners = Collections.emptyMap()

        /**
         * Convert to Hibernate properties
         *
         * @return The hibernate properties
         */
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
