package org.grails.datastore.gorm.cassandra.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.cassandra.CassandraGormEnhancer
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * Cassandra specific dynamic methods configurer
 *
 */
class CassandraMethodsConfigurer extends DynamicMethodsConfigurer {

    CassandraMethodsConfigurer(Datastore datastore) {
        this(datastore, null)
    }
        
    CassandraMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "Cassandra"
    }

    
    @Override
    protected GormEnhancer createEnhancer() {
        def ge
        if (transactionManager == null) {
            ge = new CassandraGormEnhancer(datastore)
        }
        else {
            ge = new CassandraGormEnhancer(datastore, transactionManager)
        }
        ge.failOnError = failOnError
        ge
    }
}
