/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.proxy;


import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;

/**
 * Implementation of the ProxyHandler interface for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.2.2
 */
public class HibernateProxyHandler extends SimpleHibernateProxyHandler {

    public boolean isInitialized(Object o) {
        if (o instanceof PersistentCollection) {
            return ((PersistentCollection)o).wasInitialized();
        }

        return super.isInitialized(o);
    }

    public Object unwrapIfProxy(Object instance) {
        if (instance instanceof AbstractPersistentCollection) {
            initialize(instance);
            return instance;
        }

        return super.unwrapIfProxy(instance);
    }

    public boolean isProxy(Object o) {
        return super.isProxy(o) || (o instanceof AbstractPersistentCollection);
    }

    public void initialize(Object o) {
        if (o instanceof AbstractPersistentCollection) {
            final AbstractPersistentCollection col = (AbstractPersistentCollection)o;
            if (!col.wasInitialized()) {
                col.forceInitialization();
            }
        }
        super.initialize(o);
    }
}
