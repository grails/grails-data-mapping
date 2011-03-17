package org.grails.datastore.gorm.gemfire.config

import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean

import com.gemstone.gemfire.cache.client.Pool
import com.gemstone.gemfire.cache.client.PoolFactory

/**
 * factory bean for constructing pools
 */
class PoolFactoryBean implements FactoryBean<Pool>, BeanNameAware {
    PoolFactory poolFactory
    String beanName

    Pool getObject() {
        poolFactory.create(beanName)
    }

    Class<?> getObjectType() { Pool }

    boolean isSingleton() { true }
}