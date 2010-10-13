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
package org.grails.datastore.gorm.gemfire

import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.gemfire.GemfireDatastore
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.finders.DynamicFinder
import org.springframework.datastore.mapping.gemfire.query.GemfireQuery
import com.gemstone.gemfire.cache.query.CqAttributesFactory
import com.gemstone.gemfire.cache.query.CqListener
import com.gemstone.gemfire.cache.query.CqAttributes
import com.gemstone.gemfire.cache.client.PoolManager
import com.gemstone.gemfire.cache.client.Pool

/**
 * Extended API for doing Continous queries in Gemfire
 */
class ContinuousQueryApi {

  final PersistentEntity entity
  final GemfireDatastore gemfire

  private dynamicFinders;

  ContinuousQueryApi(PersistentEntity entity, GemfireDatastore gemfire) {
    this.entity = entity
    this.gemfire = gemfire
    this.dynamicFinders = DynamicFinder.getAllDynamicFinders(gemfire)
  }

  def methodMissing(String methodName, args) {
      FinderMethod method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
      def cls = entity.javaClass
      if (method && args && (args[-1] instanceof Closure) && (method instanceof DynamicFinder)) {
          DynamicFinder dynamicFinder = method

          def invocation = dynamicFinder.createFinderInvocation(entity.javaClass, methodName, null, args)
          GemfireQuery q = dynamicFinder.buildQuery(invocation)
          def queryString = q.getQueryString()

          def cache = gemfire.gemfireCache
          def queryService = cache.getQueryService()
          Pool pool = queryService.pool
          if(pool == null) {
            def factory = PoolManager.createFactory()
            factory.addLocator("localhost",64771)
            factory.setSubscriptionEnabled(true)
            pool = factory.create(entity.name);

            queryService.pool = pool
          }

          CqAttributesFactory cqf = new CqAttributesFactory()
          cqf.addCqListener(args[-1] as CqListener)
          CqAttributes attrs = cqf.create()

          def continuousQuery = queryService.newCq("${entity.name}.${methodName}",queryString, attrs)

          continuousQuery.execute()
          gemfire.addContinuousQuery(continuousQuery)
          return continuousQuery
      }
      else {
          throw new MissingMethodException(methodName, cls, args)
      }
  }
}
