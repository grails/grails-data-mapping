package org.grails.datastore.gorm.bootstrap.support

import grails.gorm.services.Service
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.reflect.NameUtils
import org.grails.datastore.mapping.services.ServiceRegistry
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.ConfigurableBeanFactory

import java.beans.Introspector

/**
 * @author Graeme Rocher
 * @since
 */
@CompileStatic
class ServiceRegistryFactoryBean implements FactoryBean<ServiceRegistry>, BeanFactoryAware {
    final Datastore datastore

    ServiceRegistryFactoryBean(Datastore datastore) {
        this.datastore = datastore
    }

    @Override
    ServiceRegistry getObject() throws Exception {
        return datastore
    }

    @Override
    Class<?> getObjectType() {
        return ServiceRegistry
    }

    @Override
    boolean isSingleton() {
        return true
    }

    @Override
    void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if(beanFactory instanceof ConfigurableBeanFactory) {
            ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory)beanFactory
            for(service in datastore.services) {
                def serviceClass = service.getClass()
                Service ann = serviceClass.getAnnotation(Service)
                String serviceName = ann?.name()
                if(serviceName == null) {
                    serviceName = Introspector.decapitalize(serviceClass.simpleName)
                }

                if(!configurableBeanFactory.containsBean(serviceName)) {
                    configurableBeanFactory.registerSingleton(serviceName, service)
                }
                else {
                    String root = Introspector.decapitalize( datastore.getClass().simpleName - 'Datastore' )
                    serviceName = "${root}${NameUtils.capitalize(serviceName)}"
                    if(!configurableBeanFactory.containsBean(serviceName)) {
                        configurableBeanFactory.registerSingleton(serviceName, service)
                    }
                }
            }
        }
    }

}
