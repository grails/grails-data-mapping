package org.grails.datastore.mapping.services

/**
 * Represents a registry of GORM services
 *
 * @author Graeme Rocher
 * @since 6.1
 */
interface ServiceRegistry {

    /**
     * Obtain a service for the given interface
     *
     * @param interfaceType The interface type
     * @return
     */
    public <T extends Service> T getService(Class<T> interfaceType) throws ServiceNotFoundException
}