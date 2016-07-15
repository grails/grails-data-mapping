package org.grails.datastore.mapping.multitenancy.web

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.multitenancy.TenantResolver
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest

/**
 * Resolves the tenant id from the user HTTP session
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class SessionTenantResolver implements TenantResolver {

    public static final String ATTRIBUTE = "gorm.tenantId"
    /**
     * The attribute name to use
     */
    String attributeName = ATTRIBUTE

    @Override
    Serializable resolveTenantIdentifier() throws TenantNotFoundException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes()
        if(requestAttributes != null) {

            def tenantId = requestAttributes.getAttribute(attributeName, RequestAttributes.SCOPE_SESSION)
            if(tenantId instanceof Serializable) {
                return (Serializable)tenantId
            }
            else {
                throw new TenantNotFoundException()
            }
        }
        throw new TenantNotFoundException("Tenant could not be resolved outside a web request")
    }
}
