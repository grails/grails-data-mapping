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
package org.codehaus.groovy.grails.orm.hibernate.support;

import grails.core.GrailsDomainClassProperty;
import grails.persistence.support.PersistenceContextInterceptor;
import grails.util.Holders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Abstract implementation of the {@link grails.persistence.support.PersistenceContextInterceptor} interface that supports multiple data sources
 *
 * @author Graeme Rocher
 * @since 2.0.7
 */
public abstract class AbstractMultipleDataSourceAggregatePersistenceContextInterceptor implements PersistenceContextInterceptor, InitializingBean, ApplicationContextAware {

    protected List<PersistenceContextInterceptor> interceptors = new ArrayList<PersistenceContextInterceptor>();
    protected ApplicationContext applicationContext;

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
            String suffix = name.equals( GrailsDomainClassProperty.DEFAULT_DATA_SOURCE ) ? "" : "_" + name;
            String beanName = "sessionFactory" + suffix;
            if (applicationContext.containsBean(beanName)) {
                SessionFactoryAwarePersistenceContextInterceptor interceptor = createPersistenceContextInterceptor(name);
                interceptor.setSessionFactory((SessionFactory) applicationContext.getBean(beanName));
                interceptors.add(interceptor);
            }
        }
    }

    protected abstract SessionFactoryAwarePersistenceContextInterceptor createPersistenceContextInterceptor(String dataSourceName);

    @Deprecated
    public void setDataSourceNames(List<String> dataSourceNames) {
        // noop, here for compatibility
    }

    private List<String> aggregateDataSourceNames() {
        List<String> dataSourceNames = new ArrayList<String>();
        Set<String> configKeys = Holders.getConfig().keySet();
        if (configKeys.contains("dataSource") && applicationContext.containsBean("dataSource")) {
            dataSourceNames.add(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE);
        }
        for (String name : configKeys) {
            if (name.startsWith("dataSource_") && applicationContext.containsBean(name)) {
                dataSourceNames.add(name.replaceFirst("dataSource_", ""));
            }
        }
        return dataSourceNames;
    }
}