package org.grails.datastore.mapping.services

import groovy.transform.Generated
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * Created by graemerocher on 11/01/2017.
 */
class DefaultServiceRegistrySpec extends Specification {

    void "test load services into service registry"() {
        given:
        ServiceRegistry reg = new DefaultServiceRegistry(Mock(Datastore))
        reg.initialize()
        ServiceRegistry reg2 = new DefaultServiceRegistry(Mock(Datastore))
        reg2.initialize()
        expect:
        reg.getService(TestService) != null
        reg.getService(TestService).datastore != null
        reg.getService(ITestService) != null
        reg.getService(ITestService).is reg.getService(TestService)
        reg.getService(TestService) != reg2.getService(TestService)
        reg.getService(TestService).datastore != reg2.getService(TestService).datastore
    }

    void "test that all Service trait methods are marked as Generated"() {
        expect: "all Service methods are marked as Generated on implementation class"
        Service.getMethods().each { Method traitMethod ->
            assert TestService.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestService implements Service, ITestService {
}
interface ITestService {}