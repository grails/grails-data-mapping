package org.grails.datastore.gorm.riak

import org.grails.datastore.gorm.events.AutoTimestampInterceptor
import org.grails.datastore.gorm.events.DomainEventInterceptor
import org.springframework.beans.factory.FactoryBean
import org.springframework.datastore.mapping.riak.RiakDatastore
import org.springframework.datastore.mapping.model.MappingContext

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakDatastoreFactoryBean implements FactoryBean<RiakDatastore> {

  Map<String, String> config
  MappingContext mappingContext

  RiakDatastore getObject() {
    RiakDatastore datastore = new RiakDatastore(mappingContext, config)
    datastore.addEntityInterceptor(new DomainEventInterceptor())
    datastore.addEntityInterceptor(new AutoTimestampInterceptor())
    datastore.afterPropertiesSet()

    datastore
  }

  Class<?> getObjectType() { RiakDatastore }

  boolean isSingleton() { true }

}
