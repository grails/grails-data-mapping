package org.grails.datastore.gorm.cassandra.bean.factory

import com.datastax.driver.core.Cluster
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertyResolver

/**
 * @author jeff.beck on 2/13/14.
 * @author Graeme Rocher
 */
@CompileStatic
class CassandraDatastoreFactoryBean implements FactoryBean<CassandraDatastore>, ApplicationContextAware {

    CassandraMappingContext mappingContext
    PropertyResolver config
    ApplicationContext applicationContext
    boolean developmentMode = false

    @Autowired(required = false)
    Cluster cluster

    @Override
    CassandraDatastore getObject() throws Exception {
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext)applicationContext
        CassandraDatastore datastore = cluster ? new CassandraDatastore(mappingContext, config, cluster, ctx) : new CassandraDatastore(mappingContext, config, ctx)
        datastore.developmentMode = developmentMode
        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)

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
