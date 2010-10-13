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

import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.grails.datastore.gorm.finders.FindByFinder
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FindAllByFinder
import org.grails.datastore.gorm.finders.CountByFinder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionDefinition
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.springframework.datastore.mapping.model.types.OneToMany
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.gorm.finders.FindByBooleanFinder
import org.grails.datastore.gorm.finders.FindAllByBooleanFinder
import org.grails.datastore.gorm.finders.ListOrderByFinder
import org.grails.datastore.gorm.finders.FinderMethod

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
    initialiseFinders(datastore)
  }

  private List initialiseFinders(Datastore datastore) {

    this.finders = DynamicFinder.getAllDynamicFinders(datastore)
  }

  GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
    this.datastore = datastore;
    initialiseFinders(datastore)
    this.transactionManager = transactionManager
  }



  void enhance() {
    for(PersistentEntity e in datastore.mappingContext.persistentEntities) {
      enhance e
    }
  }
  void enhance(PersistentEntity e) {
    def cls = e.javaClass
    def cpf = ClassPropertyFetcher.forClass(cls)
    def staticMethods = getStaticApi(cls)
    def instanceMethods = [getInstanceApi(cls), getValidationApi(cls)]
    def tm = transactionManager

    final namedQueries = cpf.getStaticPropertyValue('namedQueries', Closure)
    if(namedQueries) {
      if(namedQueries instanceof Closure) {
        def namedQueryBuilder = new NamedQueriesBuilder(e, finders)
        namedQueryBuilder.evaluate namedQueries
      }
    }
    cls.metaClass {
      for(apiProvider in instanceMethods) {
        for(method in apiProvider.methodNames) {
          Closure methodHandle = apiProvider.&"$method"
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
      }
      for(prop in e.associations) {
        if(prop instanceof OneToMany) {
          def associatedEntity = prop.associatedEntity
          "addTo${prop.capitilizedName}" { arg ->
              def obj
              if (delegate[prop.name] == null) {
                  delegate[prop.name] = [].asType( prop.type )
              }
              if (arg instanceof Map) {
                  obj = associatedEntity.javaClass.newInstance(arg)
                  delegate[prop.name].add(obj)
              }
              else if (associatedEntity.javaClass.isInstance(arg)) {
                  obj = arg
                  delegate[prop.name].add(obj)
              }
              else {
                  throw new MissingMethodException("addTo${prop.capitilizedName}", associatedEntity.javaClass, [arg] as Object[])
              }
              if (prop.bidirectional && prop.inverseSide) {
                  def otherSide = prop.inverseSide
                  if (otherSide instanceof OneToMany) {
                      String name = otherSide.name
                      if (!obj[name]) {
                          obj[name] = [].asType(otherSide.type)
                      }
                      obj[otherSide.name].add(delegate)
                  }
                  else {
                      obj[otherSide.name] = delegate
                  }
              }
              delegate
          }
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
    def dynamicFinders = finders
    mc.static.methodMissing = {String methodName, args ->
          def method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
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

  protected GormStaticApi getStaticApi(Class cls) {
    return new GormStaticApi(cls, datastore)
  }

  protected GormInstanceApi getInstanceApi(Class cls) {
    return new GormInstanceApi(cls, datastore)
  }

  protected GormValidationApi getValidationApi(Class cls) {
    return new GormValidationApi(cls, datastore)
  }
}
