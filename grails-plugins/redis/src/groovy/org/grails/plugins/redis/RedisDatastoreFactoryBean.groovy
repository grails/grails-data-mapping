package org.grails.plugins.redis

import org.springframework.beans.factory.FactoryBean
import org.springframework.datastore.redis.RedisDatastore
import org.springframework.datastore.mapping.MappingContext
import org.grails.datastore.gorm.redis.RedisGormEnhancer
import org.springframework.transaction.PlatformTransactionManager

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 25, 2010
 * Time: 3:04:31 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisDatastoreFactoryBean implements FactoryBean<RedisDatastore>{

  Map<String, String> config
  MappingContext mappingContext
  PlatformTransactionManager transactionManager

  RedisDatastore getObject() {
    def datastore = new RedisDatastore(mappingContext, config)
    def enhancer = transactionManager ?
                        new RedisGormEnhancer(datastore, transactionManager) :
                        new RedisGormEnhancer(datastore)

    enhancer.enhance()
    return datastore
  }

  Class<?> getObjectType() { RedisDatastore }

  boolean isSingleton() { true }
}
