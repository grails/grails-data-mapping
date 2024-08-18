package org.grails.datastore.mapping.multitenancy.web

import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import spock.lang.Specification

import jakarta.servlet.http.Cookie

/**
 * Created by graemerocher on 15/07/2016.
 */
class CookieTenantResolverSpec extends Specification {

    void "Test subdomain resolver throws an exception outside a web request"() {
        when:
        new CookieTenantResolver().resolveTenantIdentifier()

        then:
        def e = thrown(TenantNotFoundException)
        e.message == "Tenant could not be resolved outside a web request"
    }


    void "Test not tenant id found"() {
        setup:
        def request = new MockHttpServletRequest("GET", "/foo")
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(request))

        when:
        new CookieTenantResolver().resolveTenantIdentifier()

        then:
        def e = thrown(TenantNotFoundException)
        e.message == "No tenantId found"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)

    }

    void "Test that the subdomain is the tenant id when a request is present"() {

        setup:
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo")
        request.cookies = new Cookie(CookieTenantResolver.COOKIE_NAME, "foo")
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(request))


        when:
        def tenantId = new CookieTenantResolver().resolveTenantIdentifier()

        then:
        tenantId == "foo"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}

