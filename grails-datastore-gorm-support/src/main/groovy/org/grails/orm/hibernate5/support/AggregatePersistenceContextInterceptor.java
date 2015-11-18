package org.grails.orm.hibernate5.support;

import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor;
import org.grails.orm.hibernate.support.SessionFactoryAwarePersistenceContextInterceptor;

/**
 * Concrete implementation of the {@link org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor} class for Hibernate 4
 *
 * @author Graeme Rocher
 * @author Burt Beckwith
 */
public class AggregatePersistenceContextInterceptor extends AbstractMultipleDataSourceAggregatePersistenceContextInterceptor {

    private AbstractHibernateDatastore hibernateDatastore;

    @Override
    protected SessionFactoryAwarePersistenceContextInterceptor createPersistenceContextInterceptor(String dataSourceName) {
        HibernatePersistenceContextInterceptor interceptor = new HibernatePersistenceContextInterceptor(dataSourceName);
        if(hibernateDatastore != null) {
            interceptor.setHibernateDatastore(hibernateDatastore);
        }
        return interceptor;
    }

    public void setHibernateDatastore(AbstractHibernateDatastore hibernateDatastore) {
        this.hibernateDatastore = hibernateDatastore;
    }

}
