package org.grails.datastore.mapping.multitenancy.web

import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import spock.lang.Specification

/**
 * Created by sdelamo on 08/09/2017.
 */
class HttpHeaderTenantResolverSpec extends Specification {

    void "Test HttpHeader resolver throws an exception outside a web request"() {
        when:
        new HttpHeaderTenantResolver().resolveTenantIdentifier()

        then:
        def e = thrown(TenantNotFoundException)
        e.message == "Tenant could not be resolved outside a web request"
    }


    void "Test not tenant id found"() {
        setup:
        def request = new MockHttpServletRequest("GET", "/foo")
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(request))

        when:
        new HttpHeaderTenantResolver().resolveTenantIdentifier()

        then:
        def e = thrown(TenantNotFoundException)
        e.message == "Tenant could not be resolved from HTTP Header: ${HttpHeaderTenantResolver.HEADER_NAME}"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)

    }

    void "Test HttpHeader value is the tenant id when a request is present"() {

        setup:
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo")
        request.addHeader(HttpHeaderTenantResolver.HEADER_NAME, "foo")
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(request))


        when:
        def tenantId = new HttpHeaderTenantResolver().resolveTenantIdentifier()

        then:
        tenantId == "foo"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}

