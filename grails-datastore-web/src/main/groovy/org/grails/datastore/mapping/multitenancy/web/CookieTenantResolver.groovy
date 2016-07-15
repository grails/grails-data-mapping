package org.grails.datastore.mapping.multitenancy.web

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.multitenancy.TenantResolver
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

/**
 * Resolves the tenant id from a cookie
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class CookieTenantResolver implements TenantResolver {

    public static final String COOKIE_NAME = "gorm.tenantId"

    /**
     * The name of the cookie
     */
    String cookieName = COOKIE_NAME

    @Override
    Serializable resolveTenantIdentifier() throws TenantNotFoundException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes()
        if(requestAttributes instanceof ServletWebRequest) {

            HttpServletRequest servletRequest = ((ServletWebRequest) requestAttributes).getRequest()
            Cookie[] cookies = servletRequest.getCookies();
            if(cookies != null) {

                for (Cookie cookie : cookies) {
                    if( cookieName.equals( cookie.name ) ) {
                        return cookie.getValue()
                    }
                }
            }
            throw new TenantNotFoundException()
        }
        throw new TenantNotFoundException("Tenant could not be resolved outside a web request")
    }
}
