package grails.gorm.multitenancy

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.multitenancy.AllTenantsResolver
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings
import org.grails.datastore.mapping.multitenancy.MultiTenantCapableDatastore
import org.grails.datastore.mapping.multitenancy.TenantResolver

/**
 * Helper methods for working with multi tenancy
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class Tenants {
    /**
     * Execute the given closure for each tenant.
     *
     * @param callable The closure
     * @return The result of the closure
     */
    static void eachTenant(Closure callable) {
        Datastore datastore = GormEnhancer.findSingleDatastore()
        eachTenantInternal(datastore, callable)
    }

    /**
     * Execute the given closure for each tenant.
     *
     * @param callable The closure
     * @return The result of the closure
     */
    static void eachTenant(Class<? extends Datastore> datastoreClass, Closure callable) {
        eachTenantInternal(GormEnhancer.findDatastoreByType(datastoreClass), callable)
    }

    /**
     * @return The current tenant id
     */
    static Serializable currentId() {
        Datastore datastore = GormEnhancer.findSingleDatastore()
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            def tenantId = CurrentTenant.get()
            if(tenantId != null) {
                return tenantId
            }
            else {
                return multiTenantCapableDatastore.getTenantResolver().resolveTenantIdentifier()
            }
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    /**
     * @return The current tenant id for the given datastore type
     */
    static Serializable currentId(Class<? extends Datastore> datastoreClass) {
        Datastore datastore = GormEnhancer.findDatastoreByType(datastoreClass)
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            def tenantId = CurrentTenant.get()
            if(tenantId != null) {
                log.debug "Found tenant id [$tenantId] bound to thread local"
                return tenantId
            }
            else {
                def tenantResolver = multiTenantCapableDatastore.getTenantResolver()
                def tenantIdentifier = tenantResolver.resolveTenantIdentifier()
                log.debug "Resolved tenant id [$tenantIdentifier] from resolver [${tenantResolver.getClass().simpleName}]"
                return tenantIdentifier
            }
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    /**
     * Execute the given closure without any tenant id. In Multi tenancy mode SINGLE this will execute against the default data source. If multi tenancy mode
     * MULTI this will execute without including the "tenantId" on any query. Use with caution.
     *
     * @param callable The closure
     * @return The result of the closure
     */
    static <T> T withoutId(Closure<T> callable) {
        Datastore datastore = GormEnhancer.findSingleDatastore()
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            return withTenantIdInternal(multiTenantCapableDatastore, ConnectionSource.DEFAULT, callable)
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    /**
     * Execute the given closure with the current tenant
     *
     * @param callable The closure
     * @return The result of the closure
     */
    static <T> T withCurrent(Closure<T> callable) {
        Datastore datastore = GormEnhancer.findSingleDatastore()
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            def tenantIdentifier = multiTenantCapableDatastore.getTenantResolver().resolveTenantIdentifier()
            return withTenantIdInternal(multiTenantCapableDatastore, tenantIdentifier, callable)
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    /**
     * Execute the given closure with the current tenant
     *
     * @param datastoreClass The datastore class
     * @param callable The closure
     * @return The result of the closure
     */
    static <T> T withCurrent(Class<? extends Datastore> datastoreClass, Closure<T> callable) {
        Datastore datastore = GormEnhancer.findDatastoreByType(datastoreClass)
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            def tenantIdentifier = multiTenantCapableDatastore.getTenantResolver().resolveTenantIdentifier()
            return withTenantIdInternal(multiTenantCapableDatastore, tenantIdentifier, callable)
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    /**
     * Execute the given closure with given tenant id
     * @param tenantId The tenant id
     * @param callable The closure
     * @return The result of the closure
     */
    static <T> T withId(Serializable tenantId, Closure<T> callable) {
        Datastore datastore = GormEnhancer.findSingleDatastore()
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            return withTenantIdInternal(multiTenantCapableDatastore, tenantId, callable)
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }
    /**
     * Execute the given closure with given tenant id
     * @param tenantId The tenant id
     * @param callable The closure
     * @return The result of the closure
     */
    static <T> T withId(Class<? extends Datastore> datastoreClass, Serializable tenantId, Closure<T> callable) {
        Datastore datastore = GormEnhancer.findDatastoreByType(datastoreClass)
        if(datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore)datastore
            return withTenantIdInternal(multiTenantCapableDatastore, tenantId, callable)
        }
        else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    private static <T> T withTenantIdInternal(MultiTenantCapableDatastore multiTenantCapableDatastore, Serializable tenantIdentifier, Closure<T> callable) {
        return CurrentTenant.withTenant(tenantIdentifier) {
            multiTenantCapableDatastore.withNewSession(tenantIdentifier) { session ->
                def i = callable.parameterTypes.length
                switch (i) {
                    case 0:
                        return callable.call()
                        break
                    case 1:
                        return callable.call(tenantIdentifier)
                        break
                    case 2:
                        return callable.call(tenantIdentifier, session)
                    default:
                        throw new IllegalArgumentException("Provided closure accepts too many arguments")
                }

            }
        }
    }

    private static void eachTenantInternal(Datastore datastore, Closure callable) {
        if (datastore instanceof MultiTenantCapableDatastore) {
            MultiTenantCapableDatastore multiTenantCapableDatastore = (MultiTenantCapableDatastore) datastore
            MultiTenancySettings.MultiTenancyMode multiTenancyMode = multiTenantCapableDatastore.multiTenancyMode
            if (multiTenancyMode == MultiTenancySettings.MultiTenancyMode.DATABASE) {
                ConnectionSources connectionSources = multiTenantCapableDatastore.connectionSources
                for (ConnectionSource connectionSource in connectionSources.allConnectionSources) {
                    def tenantId = connectionSource.name
                    if (tenantId != ConnectionSource.DEFAULT) {
                        withTenantIdInternal(multiTenantCapableDatastore, tenantId, callable)
                    }
                }
            }
            else if (multiTenancyMode.isSharedConnection()) {
                TenantResolver tenantResolver = multiTenantCapableDatastore.tenantResolver
                if (tenantResolver instanceof AllTenantsResolver) {
                    for (tenantId in ((AllTenantsResolver) tenantResolver).resolveTenantIds()) {
                        withTenantIdInternal(multiTenantCapableDatastore, tenantId, callable)
                    }
                } else {
                    throw new UnsupportedOperationException("Multi tenancy mode $multiTenancyMode is configured, but the configured TenantResolver does not implement the [org.grails.datastore.mapping.multitenancy.AllTenantsResolver] interface")
                }
            } else {
                throw new UnsupportedOperationException("Method not supported in multi tenancy mode $multiTenancyMode")
            }
        } else {
            throw new UnsupportedOperationException("Datastore implementation does not support multi-tenancy")
        }
    }

    @CompileStatic
    protected static class CurrentTenant  {

        private static final ThreadLocal<Serializable> currentTenantThreadLocal = new ThreadLocal<>()

        /**
         * @return Obtain the current tenant
         */
        static Serializable get() {
            currentTenantThreadLocal.get()
        }

        /**
         * Set the current tenant
         *
         * @param tenantId The tenant id
         */
        private static void set(Serializable tenantId) {
            currentTenantThreadLocal.set(tenantId)
        }

        private static void remove() {
            currentTenantThreadLocal.remove()
        }

        /**
         * Execute with the current tenant
         *
         * @param callable The closure
         * @return The result of the closure
         */
        static <T> T withTenant(Serializable tenantId, Closure<T> callable) {
            try {
                set(tenantId)
                callable.call(tenantId)
            } finally {
                remove()
            }
        }
    }

}
