package org.grails.orm.hibernate.support

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.orm.hibernate.cfg.HibernateMappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.env.PropertyResolver

/**
 * Builds a HibernateMappingContext
 */
@CompileStatic
class HibernateMappingContextFactoryBean implements FactoryBean<MappingContext>, ApplicationContextAware {

    PropertyResolver configuration
    ProxyFactory proxyFactory
    ApplicationContext applicationContext
    Class[] persistentClasses = [] as Class[]
    Map<String, Object> defaultConstraints

    @Override
    MappingContext getObject() throws Exception {
        def ctx = new HibernateMappingContext(configuration ?: applicationContext.getEnvironment(), applicationContext, defaultConstraints, persistentClasses)
        if(proxyFactory != null) {
            ctx.setProxyFactory(proxyFactory)
        }
        return ctx
    }

    @Override
    Class<?> getObjectType() {
        return HibernateMappingContext.class
    }

    @Override
    boolean isSingleton() {
        return true
    }
}
