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
package org.grails.datastore.gorm

import org.springframework.datastore.core.Datastore
import org.springframework.datastore.mapping.PersistentEntity
import org.grails.datastore.gorm.finders.FindByFinder
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FindAllByFinder
import org.grails.datastore.gorm.finders.CountByFinder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionDefinition

/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
class GormEnhancer {

  Datastore datastore
  PlatformTransactionManager transactionManager
  List<DynamicFinder> finders


  GormEnhancer(Datastore datastore) {
    this.datastore = datastore;
    this.finders = [new FindByFinder(datastore), new FindAllByFinder(datastore), new CountByFinder(datastore)]
  }

  GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
    this.datastore = datastore;
    this.finders = [new FindByFinder(datastore), new FindAllByFinder(datastore), new CountByFinder(datastore)]
    this.transactionManager = transactionManager
  }



  void enhance() {
    for(PersistentEntity e in datastore.mappingContext.persistentEntities) {
      enhance e.javaClass
    }
  }
  void enhance(Class cls) {
    def staticMethods = new GormStaticApi(cls,datastore)
    def instanceMethods = new GormInstanceApi(cls, datastore)
    def tm = transactionManager
    cls.metaClass {
      for(method in instanceMethods.methodNames) {
        Closure methodHandle = instanceMethods.&"$method"
        if(methodHandle.parameterTypes.size()>0) {

           // use fake object just so we have the right method signature
           Closure curried = methodHandle.curry(new Object())
          "$method"(new Closure(this) {
            def call(Object[] args) {
              methodHandle(delegate, *args)
            }
            Class[] getParameterTypes() { curried.parameterTypes }
          })
        }
      }
      'static' {
        for(method in staticMethods.methodNames) {
          delegate."$method" = staticMethods.&"$method"
        }

        if(tm) {

          withTransaction { Closure callable ->
            if(callable) {
              def transactionTemplate = new TransactionTemplate(tm)
              transactionTemplate.execute(callable as TransactionCallback)
            }
          }
          withTransaction { TransactionDefinition definition, Closure callable ->
            if(callable) {
              def transactionTemplate = new TransactionTemplate(tm, definition)
              transactionTemplate.execute(callable as TransactionCallback)
            }
          }
        }
      }
    }

    def mc = cls.metaClass
    mc.static.methodMissing = {String methodName, args ->
          def method = finders.find { DynamicFinder f -> f.isMethodMatch(methodName) }
          if (method) {
              // register the method invocation for next time
              synchronized(this) {
                  mc.static."$methodName" = {List varArgs ->
                      method.invoke(cls, methodName, varArgs)
                  }
              }
              return method.invoke(cls, methodName, args)
          }
          else {
              throw new MissingMethodException(methodName, delegate, args)
          }
     }
  }
}
