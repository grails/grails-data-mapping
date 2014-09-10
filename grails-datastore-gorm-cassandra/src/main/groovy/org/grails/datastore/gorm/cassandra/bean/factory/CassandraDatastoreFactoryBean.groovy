package org.grails.datastore.gorm.cassandra.bean.factory

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Created by jeff.beck on 2/13/14.
 */
class CassandraDatastoreFactoryBean implements FactoryBean<CassandraDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    ConfigObject config = new ConfigObject()
    ApplicationContext applicationContext

    @Override
    CassandraDatastore getObject() throws Exception {
        CassandraDatastore datastore = new CassandraDatastore(mappingContext, config, applicationContext)

        applicationContext.addApplicationListener new DomainEventListener(datastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        datastore.afterPropertiesSet()
        datastore
    }

    @Override
    Class<?> getObjectType() {
        CassandraDatastore
    }

    @Override
    boolean isSingleton() {
        true
    }
}
