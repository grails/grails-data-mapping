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


import java.util.*;

import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.grails.orm.hibernate.cfg.Mapping;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertyResolver;

/**
 * Abstract implementation of the {@link PersistenceContextInterceptor} interface that supports multiple data sources
 *
 * @author Graeme Rocher
 * @since 2.0.7
 */
public abstract class AbstractMultipleDataSourceAggregatePersistenceContextInterceptor implements PersistenceContextInterceptor, InitializingBean, ApplicationContextAware {

    public static final String SESSION_FACTORY_BEAN_NAME = "sessionFactory";
    public static final String DEFAULT_DATA_SOURCE_NAME = "dataSource";
    public static final String DATA_SOURCES = "dataSources";

    protected List<PersistenceContextInterceptor> interceptors = new ArrayList<PersistenceContextInterceptor>();
    protected ApplicationContext applicationContext;
    protected PropertyResolver config;


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

    @Deprecated
    public void setDataSourceNames(List<String> dataSourceNames) {
        // noop, here for compatibility
    }

    private Set<String> aggregateDataSourceNames() {
        if(applicationContext == null) return Collections.emptySet();
        Set<String> resolvedDataSourceNames = new HashSet<String>();
        Set<String> dataSourceNames = calculateDataSourceNames(this.config);
        for (String dataSourceName : dataSourceNames) {
            if (applicationContext.containsBean(dataSourceName.equals(Mapping.DEFAULT_DATA_SOURCE) ? "dataSource" : "dataSource_" + dataSourceName)) {
                resolvedDataSourceNames.add(dataSourceName);
            }
        }
        return resolvedDataSourceNames;
    }

    private static Set<String> datasourceNames = null;
    public static Set<String> calculateDataSourceNames(PropertyResolver config) {
        if(datasourceNames != null) return datasourceNames;

        datasourceNames = new HashSet<String>();

        if(config == null) {
            return datasourceNames;
        }


        Map dataSources = config.getProperty(DATA_SOURCES, Map.class, Collections.emptyMap());

        if (dataSources != null && !dataSources.isEmpty()) {
            for (Object name : dataSources.keySet()) {
                String nameAsString = name.toString();
                if (nameAsString.equals( DEFAULT_DATA_SOURCE_NAME) ) {
                    datasourceNames.add( Mapping.DEFAULT_DATA_SOURCE );
                } else {
                    datasourceNames.add( nameAsString );
                }
            }
        } else {
            Map dataSource = (Map)config.getProperty(DEFAULT_DATA_SOURCE_NAME, Map.class, Collections.emptyMap());
            if (dataSource != null && !dataSource.isEmpty()) {
                datasourceNames.add( Mapping.DEFAULT_DATA_SOURCE);
            }
        }
        return datasourceNames;
    }
}