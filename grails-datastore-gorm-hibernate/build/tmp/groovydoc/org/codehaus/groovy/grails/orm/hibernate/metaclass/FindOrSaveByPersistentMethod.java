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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.hibernate.SessionFactory;

public class FindOrSaveByPersistentMethod extends FindOrCreateByPersistentMethod {

    private static final String METHOD_PATTERN = "(findOrSaveBy)([A-Z]\\w*)";

    /**
     * Constructor.
     * @param application
     * @param sessionFactory
     * @param classLoader
     */
    public FindOrSaveByPersistentMethod(HibernateDatastore datastore,GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader) {
        super(datastore, application,sessionFactory, classLoader, METHOD_PATTERN);
    }

    @Override
    protected boolean shouldSaveOnCreate() {
        return true;
    }
}
