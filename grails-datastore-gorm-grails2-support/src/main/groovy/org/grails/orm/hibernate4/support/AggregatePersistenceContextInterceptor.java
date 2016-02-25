/*
 * Copyright 2011 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate4.support;

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
