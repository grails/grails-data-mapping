package org.grails.datastore.mapping.core.connections

import groovy.transform.AutoClone
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

import javax.persistence.FlushModeType

/**
 * Default settings shared across all implementations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
@AutoClone
class ConnectionSourceSettings {

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
     */
    boolean autoFlush = false

    /**
     * Whether to fail on a validation error
     */
    boolean failOnError = false

    private final Defaults defaults = new Defaults()

    /**
     * @return Any defaults
     */
    Defaults getDefault() {
        return this.defaults
    }

    /**
     * Represents the default settings
     */
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class Defaults {
        /**
         * The default mapping
         */
        Closure mapping

        /**
         * The default constraints
         */
        Closure constraints
    }
}

