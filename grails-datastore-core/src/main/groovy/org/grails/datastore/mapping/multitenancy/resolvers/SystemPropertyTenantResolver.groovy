package org.grails.datastore.mapping.multitenancy.resolvers

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.multitenancy.TenantResolver
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException

/**
 * A {@link TenantResolver} that resolves from a System property called "gorm.tenantId". Useful for testing.
 */
@CompileStatic
class SystemPropertyTenantResolver implements TenantResolver {

    public static final String PROPERTY_NAME = "gorm.tenantId"

    @Override
    Serializable resolveTenantIdentifier(Class persistentClass) throws TenantNotFoundException {
        def value = System.getProperty(PROPERTY_NAME)
        if(value) {
            return value
        }
        else {
            throw new TenantNotFoundException(persistentClass)
        }
    }
}
