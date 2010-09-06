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

import org.springframework.datastore.proxy.ProxyFactory
import org.springframework.datastore.core.Session
import org.springframework.datastore.engine.EntityAccess
import org.springframework.datastore.engine.EntityPersister

/**
 * Implements the proxy interface and creates a Groovy proxy by passing the need for javassist style proxies
 * and all the problems they bring.
 *
 * @author Graeme Rocher
 */
class GroovyProxyFactory implements ProxyFactory{

  boolean isProxy(Object object) {
      object.metaClass.getMetaMethod("isProxy", null) != null
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
             if(target == null) target = session.retrieve(type, key)
             return target
          case 'isInitialized':
             return target != null
          case 'getTarget':
             if(target == null) target = session.retrieve(type, key)
             return target
          default:
            if(target == null) target = session.retrieve(type, key)
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
           if(target == null) target = session.retrieve(type, key)
           return target
         default:
           if(target == null) target = session.retrieve(type, key)
           return target[name]
       }
    }
    proxy.metaClass.setProperty = { String name, value ->
        if(target == null) target = session.retrieve(type, key)
        target[name] = value
    }
    return proxy;
  }
}
