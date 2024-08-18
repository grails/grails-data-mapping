package org.grails.datastore.mapping.core.connections

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.config.Settings
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings

import jakarta.persistence.FlushModeType

/**
 * Default settings shared across all implementations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
@AutoClone
@CompileStatic
class ConnectionSourceSettings implements Settings {

    /**
     * The class used to create
     */
    Class<ConnectionSources> connectionSourcesClass

    /**
     * The flush mode type, if any
     */
    FlushModeType flushMode = FlushModeType.AUTO

    /**
     * Whether to auto flush
     *
     * @deprecated Enabling this is generally the wrong thing to do as it will degrade performance. Hence deprecated
     */
    @Deprecated
    boolean autoFlush = false

    /**
     * Whether to autowire entities via Spring if used
     */
    boolean autowire = false

    /**
     * Whether to fail on a validation error
     */
    boolean failOnError = false

    /**
     * Whether to mark instances dirty on explicit save()
     */
    Boolean markDirty

    /**
     * Package names that should fail on error
     */
    List<String> failOnErrorPackages = Collections.emptyList()

    /**
     * Custom settings
     */
    CustomSettings custom = new CustomSettings()

    /**
     * @return Any defaults
     */
    DefaultSettings defaults = new DefaultSettings()

    /**
     * The settings for Multi Tenancy
     */
    MultiTenancySettings multiTenancy = new MultiTenancySettings()

    /**
     * @return Any defaults
     */
    DefaultSettings getDefault() {
        return this.defaults
    }

    void setDefault(DefaultSettings defaults) {
        this.defaults = defaults
    }


    /**
     * Represents the default settings
     */
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class DefaultSettings {
        /**
         * The default mapping
         */
        Closure mapping

        /**
         * The default constraints
         */
        Closure constraints
    }

    /**
     * Any custom settings
     */
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class CustomSettings {
        /**
         * custom types
         */
        List<CustomTypeMarshaller> types = []
    }
}

