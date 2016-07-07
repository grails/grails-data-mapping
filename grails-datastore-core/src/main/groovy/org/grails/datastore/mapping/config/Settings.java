package org.grails.datastore.mapping.config;

/**
 * Common settings across all GORM implementations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface Settings {
    /**
     * The default prefix
     */
    String PREFIX = "grails.gorm";
    /**
     * Whether to flush the session between each query
     */
    String SETTING_AUTO_FLUSH = PREFIX + '.' + "autoFlush";
    /**
     * The default flush mode
     */
    String SETTING_FLUSH_MODE = PREFIX + '.' + "flushMode";
    /**
     * Whether to throw an exception on a validation error
     */
    String SETTING_FAIL_ON_ERROR = PREFIX + '.' + "failOnError";
    /**
     * The default mapping
     */
    String SETTING_DEFAULT_MAPPING = PREFIX + '.' + "default.mapping";
    /**
     * The default constraints
     */
    String SETTING_DEFAULT_CONSTRAINTS = PREFIX + '.' + "default.constraints";
    /**
     * The custom types
     */
    String SETTING_CUSTOM_TYPES = PREFIX + '.' + "custom.types";

    /**
     * The multi tenancy mode
     */
    String SETTING_MULTI_TENANCY_MODE = PREFIX + '.' + "multiTenancy.mode";

    /**
     * The multi tenancy resolver class
     */
    String SETTING_MULTI_TENANT_RESOLVER_CLASS = PREFIX + '.' + "multiTenancy.tenantResolverClass";
}
