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

import org.grails.datastore.gorm.GormInstanceApi
import org.hibernate.SessionFactory

@CompileStatic
class AbstractHibernateGormInstanceApi<D> extends GormInstanceApi<D> {
    protected static final Object[] EMPTY_ARRAY = []

    protected SessionFactory sessionFactory
    protected ClassLoader classLoader
    protected boolean cacheQueriesByDefault = false

    Map config = Collections.emptyMap()

    protected AbstractHibernateGormInstanceApi(Class<D> persistentClass, AbstractHibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)
        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()
    }

    protected boolean shouldFlush(Map map = [:]) {
        if (map?.containsKey('flush')) {
            return Boolean.TRUE == map.flush
        }
        return config?.autoFlush instanceof Boolean ? config.autoFlush : false
    }
}
