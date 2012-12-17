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

import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.datastore.mapping.core.Session

import org.grails.datastore.mapping.engine.EntityPersister
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.DataIntegrityViolationException

/**
 * Implements the proxy interface and creates a Groovy proxy by passing the need for javassist style proxies
 * and all the problems they bring.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("unchecked")
class GroovyProxyFactory implements ProxyFactory {

    boolean isProxy(Object object) {
        object != null && object.metaClass.getMetaMethod("isProxy", null) != null
    }

    Serializable getIdentifier(Object obj) {
        return obj.getId()
    }

    def createProxy(Session session, Class type, Serializable key) {
        EntityPersister persister = session.getPersister(type)

        def proxy = type.newInstance()
        persister.setObjectIdentifier(proxy, key)
        def target = null
        proxy.metaClass.isProxy = {-> true}
        proxy.metaClass.invokeMethod = { String name, args ->
            switch(name) {
                case "getId":
                    return key
                case 'initialize':
                    if (target == null) target = session.retrieve(type, key)
                    return target
                case 'isInitialized':
                    return target != null
                case 'getTarget':
                    if (target == null) target = session.retrieve(type, key)
                    return target
                default:
                    if (target == null) target = session.retrieve(type, key)
                    return target."$name"(*args)
            }
        }

        proxy.metaClass.getProperty = { String name ->
            switch(name) {
                case 'id':
                return key
            case 'initialized':
                return target != null
            case 'target':
                if (target == null) target = session.retrieve(type, key)
                return target
            default:
                if (target == null) target = session.retrieve(type, key)

                if(target == null) {
                    throw new DataIntegrityViolationException("Error loading association [$key] of type [$type]. Associated instance no longer exists.")
                }
                return target[name]
            }
        }

        proxy.metaClass.setProperty = { String name, value ->
            if (target == null) target = session.retrieve(type, key)
            if(target == null) {
                throw new DataIntegrityViolationException("Error loading association [$key] of type [$type]. Associated instance no longer exists.")
            }

            target[name] = value
        }
        return proxy;
    }

    @Override
    boolean isInitialized(Object object) {
        if(isProxy(object)) {
            return object.initialized
        }
        return true;
    }

    @Override
    Object unwrap(Object object) {
        if(isProxy(object)) {
            return object.target
        }
        return object
    }
}
