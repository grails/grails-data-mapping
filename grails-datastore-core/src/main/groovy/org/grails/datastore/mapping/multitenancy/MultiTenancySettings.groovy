package org.grails.datastore.mapping.multitenancy

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.multitenancy.resolvers.NoTenantResolver
import org.springframework.beans.BeanUtils

/**
 * Represents the multi tenancy settings
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class MultiTenancySettings {

    private TenantResolver tenantResolver

    /**
     * The default multi tenancy mode
     */
    MultiTenancyMode mode = MultiTenancyMode.NONE

    /**
     * The tenant resolver class
     */

    Class<? extends TenantResolver> tenantResolverClass

    /**
     * @return The tenant resolver
     */
    TenantResolver getTenantResolver() {
        if(tenantResolver != null) {
            return tenantResolver
        }
        else if(tenantResolverClass != null) {
            return BeanUtils.instantiate(tenantResolverClass)
        }
        return new NoTenantResolver()
    }

    /**
     * Sets the tenant resolver to use
     *
     * @param tenantResolver The tenant resolver to use
     */
    void setTenantResolver(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver
    }

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
