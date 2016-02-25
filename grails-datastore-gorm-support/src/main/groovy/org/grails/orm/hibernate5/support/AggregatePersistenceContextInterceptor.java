package org.grails.orm.hibernate5.support;

import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor;
import org.grails.orm.hibernate.support.SessionFactoryAwarePersistenceContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Concrete implementation of the {@link org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor} class for Hibernate 4
 *
 * @author Graeme Rocher
 * @author Burt Beckwith
 */
public class AggregatePersistenceContextInterceptor extends AbstractMultipleDataSourceAggregatePersistenceContextInterceptor {

    private AbstractHibernateDatastore[] hibernateDatastores;

    @Override
    protected SessionFactoryAwarePersistenceContextInterceptor createPersistenceContextInterceptor(String dataSourceName) {
        HibernatePersistenceContextInterceptor interceptor = new HibernatePersistenceContextInterceptor(dataSourceName);
        if(hibernateDatastores != null) {
            for (AbstractHibernateDatastore hibernateDatastore : hibernateDatastores) {
                if(dataSourceName.equals(hibernateDatastore.getDataSourceName())) {
                    interceptor.setHibernateDatastore(hibernateDatastore);
                }
            }
        }
        return interceptor;
    }

    @Autowired
    public void setHibernateDatastores(AbstractHibernateDatastore[] hibernateDatastores) {
        this.hibernateDatastores = hibernateDatastores;
    }

}
