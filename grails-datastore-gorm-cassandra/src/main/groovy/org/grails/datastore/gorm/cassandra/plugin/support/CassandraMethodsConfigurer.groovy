package org.grails.datastore.gorm.cassandra.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.cassandra.CassandraGormEnhancer
import org.grails.datastore.gorm.cassandra.CassandraGormInstanceApi
import org.grails.datastore.gorm.cassandra.CassandraGormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

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
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new CassandraGormStaticApi(cls, datastore, finders)
    }
    
    @Override
    protected GormInstanceApi createGormInstanceApi(Class cls) {
        final api = new CassandraGormInstanceApi(cls, datastore)
        api.failOnError = failOnError
        return api
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
