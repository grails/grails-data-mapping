package org.grails.orm.hibernate.support

import org.grails.orm.hibernate.AbstractHibernateDatastore
import org.springframework.beans.factory.FactoryBean

import javax.sql.DataSource

/**
 * Created by jameskleeh on 1/13/17.
 */
class DataSourceFactoryBean implements FactoryBean<DataSource> {

    AbstractHibernateDatastore datastore
    String connectionName

    DataSourceFactoryBean(AbstractHibernateDatastore datastore, String connectionName) {
        this.datastore = datastore
        this.connectionName = connectionName
    }

    @Override
    DataSource getObject() throws Exception {
        (DataSource)datastore.connectionSources.getConnectionSource(connectionName).source
    }

    @Override
    Class<?> getObjectType() {
        DataSource
    }

    @Override
    boolean isSingleton() {
        true
    }
}
