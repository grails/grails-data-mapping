/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.proxy

import groovy.transform.CompileStatic

import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.proxy.ProxyFactory

/**
 * Implements the proxy interface and creates a Groovy proxy by passing the need for javassist style proxies
 * and all the problems they bring.
 *
 * @author Graeme Rocher
 */
@CompileStatic
class GroovyProxyFactory implements ProxyFactory {
    /**
     * Check our object has the correct meta class to be a proxy of this type.
     * @param object The object.
     * @return true if it is.
     */
    @Override
    boolean isProxy(Object object) {
        getProxyInstanceMetaClass(object) != null
    }

    protected ProxyInstanceMetaClass getProxyInstanceMetaClass(object) {
        (object != null && object.metaClass instanceof ProxyInstanceMetaClass) ? (ProxyInstanceMetaClass)object.metaClass : null
    }

    @Override
    Serializable getIdentifier(Object obj) {
        ProxyInstanceMetaClass proxyMc = getProxyInstanceMetaClass(obj)
        if (proxyMc != null) {
            return proxyMc.getKey()
        } else {
            getIdDynamic(obj)
        }
    }

    @groovy.transform.CompileDynamic
    protected Serializable getIdDynamic(obj) {
        return obj.getId()
    }

    /**
     * Creates a proxy
     *
     * @param <T> The type of the proxy to create
     * @param session The session instance
     * @param type The type of the proxy to create
     * @param key The key to proxy
     * @return A proxy instance
     */
    @Override
    public <T> T createProxy(Session session, Class<T> type, Serializable key) {
        EntityPersister persister = (EntityPersister) session.getPersister(type)
        T proxy = type.newInstance()
        persister.setObjectIdentifier(proxy, key)

        MetaClass metaClass = new ProxyInstanceMetaClass(proxy.getMetaClass(), session, key)
        if(proxy instanceof GroovyObject) {
            // direct assignment of MetaClass to GroovyObject
			((GroovyObject)proxy).setMetaClass(metaClass)
        } else {
        	// call DefaultGroovyMethods.setMetaClass
        	proxy.setMetaClass(metaClass)
        }
        return proxy
    }

    @Override
    boolean isInitialized(Object object) {
        ProxyInstanceMetaClass proxyMc = getProxyInstanceMetaClass(object)
        if (proxyMc != null) {
            return proxyMc.isProxyInitiated()
        }
        return true
    }

    @Override
    Object unwrap(Object object) {
        ProxyInstanceMetaClass proxyMc = getProxyInstanceMetaClass(object)
        if (proxyMc != null) {
            return proxyMc.getProxyTarget()
        }
        return object
    }
}
