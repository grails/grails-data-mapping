package org.grails.datastore.mapping.services

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.Datastore
import org.springframework.util.ClassUtils

/**
 * The default {@link ServiceRegistry} implementation
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class DefaultServiceRegistry implements ServiceRegistry {
    /**
     * The datastore this service relates to
     */
    final Datastore datastore
    protected final Map<Class,Service> servicesByInterface

    DefaultServiceRegistry(Datastore datastore) {
        this.datastore = datastore
        Iterable<Service> services = loadServices()
        Map<Class,Service> serviceMap = [:]
        for(Service service in services) {
            service.datastore = datastore
            def allInterfaces = ClassUtils.getAllInterfaces(service)
            serviceMap.put(service.getClass(), service)
            for(Class i in allInterfaces) {
                if(isValidInterface(i)) {
                    serviceMap.put(i, service)
                }
            }
        }

        servicesByInterface = Collections.unmodifiableMap(
            serviceMap
        )
    }

    @Override
    def <T extends Service> T getService(Class<T> interfaceType) throws ServiceNotFoundException {
        Service s = servicesByInterface.get(interfaceType)
        if(s == null) {
            throw new ServiceNotFoundException("No service found for type $interfaceType")
        }
        return (T) s
    }

    protected boolean isValidInterface(Class i) {
        i != Service && i != GroovyObject
    }

    protected Iterable<Service> loadServices() {
        ServiceLoader.load(Service)
    }
}
