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


import grails.core.support.proxy.EntityProxyHandler;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.proxy.LazyInitializer;

/**
 * Implementation of the ProxyHandler interface for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.2.2
 */
public class SimpleHibernateProxyHandler implements EntityProxyHandler {

    public boolean isInitialized(Object o) {
        if (o instanceof HibernateProxy) {
            return !((HibernateProxy)o).getHibernateLazyInitializer().isUninitialized();
        }
        return true;
    }

    public boolean isInitialized(Object obj, String associationName) {
        try {
            Object proxy = ClassPropertyFetcher.forClass(obj.getClass()).getPropertyValue(obj, associationName);
            return Hibernate.isInitialized(proxy);
        }
        catch (RuntimeException e) {
            return false;
        }
    }

    public Object unwrapIfProxy(Object instance) {
        if (instance instanceof HibernateProxy) {
            final HibernateProxy proxy = (HibernateProxy)instance;
            return unwrapProxy(proxy);
        }
        return instance;
    }

    public Object unwrapProxy(final HibernateProxy proxy) {
        final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
        if (lazyInitializer.isUninitialized()) {
            lazyInitializer.initialize();
        }
        final Object obj = lazyInitializer.getImplementation();
        if (obj != null) {
            ensureCorrectGroovyMetaClass(obj, obj.getClass());
        }
        return obj;
    }

    /**
     * Ensures the meta class is correct for a given class
     *
     * @param target The GroovyObject
     * @param persistentClass The persistent class
     */
    private static void ensureCorrectGroovyMetaClass(Object target, Class<?> persistentClass) {
        if (target instanceof GroovyObject) {
            GroovyObject go = ((GroovyObject)target);
            if (!go.getMetaClass().getTheClass().equals(persistentClass)) {
                go.setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(persistentClass));
            }
        }
    }


    public HibernateProxy getAssociationProxy(Object obj, String associationName) {
        try {
            Object proxy = ClassPropertyFetcher.forClass(obj.getClass()).getPropertyValue(obj, associationName);
            if (proxy instanceof HibernateProxy) {
                return (HibernateProxy) proxy;
            }
            return null;
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    public boolean isProxy(Object o) {
        return (o instanceof HibernateProxy);
    }

    public void initialize(Object o) {
        if (o instanceof HibernateProxy) {
            final LazyInitializer hibernateLazyInitializer = ((HibernateProxy)o).getHibernateLazyInitializer();
            if (hibernateLazyInitializer.isUninitialized()) {
                hibernateLazyInitializer.initialize();
            }
        }
    }

    public Object getProxyIdentifier(Object o) {
        if (o instanceof HibernateProxy) {
            return ((HibernateProxy)o).getHibernateLazyInitializer().getIdentifier();
        }
        return null;
    }

    public Class<?> getProxiedClass(Object o) {
        return HibernateProxyHelper.getClassWithoutInitializingProxy(o);
    }
}
