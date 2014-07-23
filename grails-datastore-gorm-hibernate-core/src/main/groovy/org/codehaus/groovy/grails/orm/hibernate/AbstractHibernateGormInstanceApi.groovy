/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.orm.hibernate.proxy.SimpleHibernateProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.grails.datastore.gorm.GormInstanceApi
import org.hibernate.FlushMode
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.proxy.HibernateProxy
import org.springframework.dao.DataAccessException

@CompileStatic
class AbstractHibernateGormInstanceApi<D> extends GormInstanceApi<D> {
    protected static final Object[] EMPTY_ARRAY = []

    protected SessionFactory sessionFactory
    protected ClassLoader classLoader
    protected boolean cacheQueriesByDefault = false
    protected IHibernateTemplate hibernateTemplate
    protected ProxyHandler proxyHandler = new SimpleHibernateProxyHandler()
    Map config = Collections.emptyMap()

    protected AbstractHibernateGormInstanceApi(Class<D> persistentClass, AbstractHibernateDatastore datastore, ClassLoader classLoader, IHibernateTemplate hibernateTemplate) {
        super(persistentClass, datastore)
        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()
        this.hibernateTemplate = hibernateTemplate
    }

    protected boolean shouldFlush(Map map = [:]) {
        if (map?.containsKey('flush')) {
            return Boolean.TRUE == map.flush
        }
        return config?.autoFlush instanceof Boolean ? config.autoFlush : false
    }

    @Override
    void discard(D instance) {
        hibernateTemplate.evict instance
    }

    @Override
    void delete(D instance, Map params = Collections.emptyMap()) {
        boolean flush = shouldFlush(params)
        try {
            hibernateTemplate.execute { Session session ->
                session.delete instance
                if(flush) {
                    session.flush()
                }
            }
        }
        catch (DataAccessException e) {
            try {
                hibernateTemplate.execute { Session session ->
                    session.flushMode = FlushMode.MANUAL
                }
            }
            finally {
                throw e
            }
        }
    }

    @Override
    boolean isAttached(D instance) {
        hibernateTemplate.contains instance
    }

    @Override
    boolean instanceOf(D instance, Class cls) {
        return proxyHandler.unwrapIfProxy(instance) in cls
    }

    @Override
    D lock(D instance) {
        hibernateTemplate.lock(instance, LockMode.PESSIMISTIC_WRITE)
    }

    @Override
    D attach(D instance) {
        hibernateTemplate.lock(instance, LockMode.NONE)
        return instance
    }

    @Override
    D refresh(D instance) {
        hibernateTemplate.refresh(instance)
        return instance
    }

}
