package org.grails.datastore.gorm.gemfire.config

/**
 * factory bean for constructing pools
 */
import com.gemstone.gemfire.cache.client.*
import org.springframework.beans.factory.*

class PoolFactoryBean implements FactoryBean<Pool>,BeanNameAware {
	PoolFactory poolFactory
	String beanName

	Pool getObject() {
		poolFactory.create(beanName)
	}

	Class<?> getObjectType() { Pool }

	boolean isSingleton() { true }
}