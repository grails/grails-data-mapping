package org.grails.datastore.mapping.multitenancy.web

import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import spock.lang.Specification

/**
 * Created by graemerocher on 08/07/2016.
 */
class SubDomainTenantResolverSpec extends Specification {

    void "Test subdomain resolver throws an exception outside a web request"() {
        when:
        new SubDomainTenantResolver().resolveTenantIdentifier(null)

        then:
        def e = thrown(TenantNotFoundException)
        e.message == "Tenant could not be resolved outside a web request"
    }

    void "Test that the subdomain is the tenant id when a request is present"() {

        setup:
        def request = new MockHttpServletRequest("GET", "/foo")
        RequestContextHolder.setRequestAttributes(new ServletWebRequest(request))

        when:
        def tenantId = new SubDomainTenantResolver().resolveTenantIdentifier(null)

        then:
        tenantId == SubDomainTenantResolver.DEFAULT_SUB_DOMAIN

        when:
        request.setServerName("foo.mycompany.com")
        tenantId = new SubDomainTenantResolver().resolveTenantIdentifier(null)

        then:
        tenantId == "foo"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}
