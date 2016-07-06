/*
 * Copyright 2013 SpringSource.
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
package org.grails.orm.hibernate.support;

import grails.config.Config;
import grails.core.GrailsApplication;
import grails.core.support.GrailsConfigurationAware;
import grails.persistence.support.PersistenceContextInterceptor;

import java.util.*;

import org.grails.orm.hibernate.cfg.Mapping;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertyResolver;

import javax.annotation.PreDestroy;

/**
 * Abstract implementation of the {@link grails.persistence.support.PersistenceContextInterceptor} interface that supports multiple data sources
 *
 * @author Graeme Rocher
 * @since 2.0.7
 */
public abstract class AbstractMultipleDataSourceAggregatePersistenceContextInterceptor implements PersistenceContextInterceptor, InitializingBean, ApplicationContextAware {



    protected List<PersistenceContextInterceptor> interceptors = new ArrayList<PersistenceContextInterceptor>();
    protected ApplicationContext applicationContext;
    protected PropertyResolver config;
    protected Set<String> dataSourceNames;

    public void setDataSourceNames(Set<String> dataSourceNames) {
        this.dataSourceNames = dataSourceNames;
    }

    public void setConfiguration(PropertyResolver co) {
        this.config = co;
    }

    public boolean isOpen() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            if (interceptor.isOpen()) {
                // true at least one is true
                return true;
            }
        }
        return false;
    }

    public void reconnect() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.reconnect();
        }
    }

    public void destroy() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            try {
                if (interceptor.isOpen()) {
                    interceptor.destroy();
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
    }

    public void clear() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.clear();
        }
    }

    public void disconnect() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.disconnect();
        }
    }

    public void flush() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.flush();
        }
    }

    public void init() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.init();
        }
    }

    public void setReadOnly() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.setReadOnly();
        }
    }

    public void setReadWrite() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.setReadWrite();
        }
    }

    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    @Override
    public void afterPropertiesSet() {
        // need to lazily create these instead of registering as beans since GrailsPageFilter
        // looks for instances of PersistenceContextInterceptor and picks one assuming
        // there's only one, so this one has to be the only one
        for (String name : aggregateDataSourceNames()) {
            String suffix = name.equals( Mapping.DEFAULT_DATA_SOURCE ) ? "" : "_" + name;
            String beanName = "sessionFactory" + suffix;
            if (applicationContext.containsBean(beanName)) {
                SessionFactoryAwarePersistenceContextInterceptor interceptor = createPersistenceContextInterceptor(name);
                interceptor.setSessionFactory((SessionFactory) applicationContext.getBean(beanName));
                interceptors.add(interceptor);
            }
        }
    }

    protected abstract SessionFactoryAwarePersistenceContextInterceptor createPersistenceContextInterceptor(String dataSourceName);


    private Set<String> aggregateDataSourceNames() {
        Set<String> resolvedDataSourceNames = new HashSet<String>();
        for (String dataSourceName : dataSourceNames) {
            if (applicationContext.containsBean(dataSourceName.equals(Mapping.DEFAULT_DATA_SOURCE) ? "dataSource" : "dataSource_" + dataSourceName)) {
                resolvedDataSourceNames.add(dataSourceName);
            }
        }
        return resolvedDataSourceNames;
    }

}