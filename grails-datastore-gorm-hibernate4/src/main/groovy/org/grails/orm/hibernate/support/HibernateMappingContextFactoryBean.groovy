package org.grails.orm.hibernate.support

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.orm.hibernate.cfg.HibernateMappingContext
import org.springframework.core.env.PropertyResolver

/**
 * Builds a HibernateMappingContext
 */
@CompileStatic
class HibernateMappingContextFactoryBean extends AbstractMappingContextFactoryBean{

    PropertyResolver configuration
    ProxyFactory proxyFactory

    @Override
    protected MappingContext createMappingContext() {
        def ctx = new HibernateMappingContext(configuration ?: applicationContext.getEnvironment(), applicationContext)
        if(proxyFactory != null) {
            ctx.setProxyFactory(proxyFactory)
        }
        return ctx
    }
}
