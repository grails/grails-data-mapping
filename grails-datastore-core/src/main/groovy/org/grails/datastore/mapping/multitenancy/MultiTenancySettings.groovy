package org.grails.datastore.mapping.multitenancy

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Represents the multi tenancy settings
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class MultiTenancySettings {

    /**
     * The default multi tenancy mode
     */
    MultiTenancyMode mode = MultiTenancyMode.NONE

    /**
     * The tenant resolver class
     */
    Class<? extends TenantResolver> tenantResolverClass

    static enum MultiTenancyMode {
        /**
         * No multi tenancy
         */
        NONE,
        /**
         * A single database per tenant
         */
        SINGLE,
        /**
         * A shared database amongst multiple tenants
         */
        MULTI
    }
}
