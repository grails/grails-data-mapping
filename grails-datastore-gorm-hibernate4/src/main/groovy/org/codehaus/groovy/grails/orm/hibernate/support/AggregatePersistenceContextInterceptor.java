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
package org.codehaus.groovy.grails.orm.hibernate.support;

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.hibernate.SessionFactory;

/**
 * @author Burt Beckwith
 */
public class AggregatePersistenceContextInterceptor extends AbstractAggregatePersistenceContextInterceptor {

    public void afterPropertiesSet() {
        // need to lazily create these instead of registering as beans since GrailsPageFilter
        // looks for instances of PersistenceContextInterceptor and picks one assuming
        // there's only one, so this one has to be the only one
        for (String name : dataSourceNames) {
            String suffix = name == GrailsDomainClassProperty.DEFAULT_DATA_SOURCE ? "" : "_" + name;
            HibernatePersistenceContextInterceptor interceptor = new HibernatePersistenceContextInterceptor();
            String beanName = "sessionFactory" + suffix;
            if (applicationContext.containsBean(beanName)) {
                interceptor.setSessionFactory((SessionFactory)applicationContext.getBean(beanName));
            }
            interceptors.add(interceptor);
        }
    }
}
