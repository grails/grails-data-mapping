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

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.springframework.datastore.mapping.gemfire.GemfireDatastore
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.data.gemfire.GemfireTemplate
import org.springframework.data.gemfire.GemfireCallback
import com.gemstone.gemfire.cache.Region
import org.springframework.datastore.mapping.query.order.ManualEntityOrdering
import org.springframework.datastore.mapping.query.Query
import com.gemstone.gemfire.cache.execute.FunctionService
import com.gemstone.gemfire.cache.execute.FunctionAdapter
import com.gemstone.gemfire.cache.execute.FunctionContext
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper
import com.gemstone.gemfire.cache.execute.RegionFunctionContext
import com.gemstone.gemfire.cache.execute.ResultSender

/**
 * Extends the default GORM capabilities adding Gemfire specific methods
 */
class GemfireGormEnhancer extends GormEnhancer{

  GemfireGormEnhancer(GemfireDatastore datastore) {
    super(datastore);
  }

  GemfireGormEnhancer(GemfireDatastore datastore, transactionManager) {
    super(datastore, transactionManager);
  }

  protected GormStaticApi getStaticApi(Class cls) {
    return new GemfireStaticApi(cls, (GemfireDatastore)datastore)
  }


}

/**
 * Wrapper for invoking Gemfire functions
 */
class ClosureInvokingFunction extends FunctionAdapter {

  def callable
  String id

  ClosureInvokingFunction(callable, id) {
    this.callable = callable;
    this.id = id;
  }

  ClosureInvokingFunction(callable) {
    this.callable = callable;
    this.id = callable.getClass().name
  }

  void execute(FunctionContext functionContext) {
    def helper = new FunctionContextHelper(functionContext)
    callable.delegate = helper
    callable.resolveStrategy = Closure.DELEGATE_FIRST
    callable?.call(functionContext)
  }


}

class FunctionContextHelper implements RegionFunctionContext, ResultSender {
  @Delegate RegionFunctionContext context
  @Delegate ResultSender resultSender
  FunctionContextHelper(RegionFunctionContext context) {
    this.context = context;
    this.resultSender = context.resultSender
  }

  def getLocalData() {
    PartitionRegionHelper.getLocalDataForContext(context)
  }

  Set<?> getFilter() {
    return context.filter
  }

  Region getDataSet() {
    return context.dataSet
  }

  Serializable getArguments() {
    return context.arguments
  }

  String getFunctionId() {
    return context.functionId
  }

  void sendResult(Serializable t) {
    resultSender.sendResult t
  }

  void lastResult(Serializable t) {
    resultSender.lastResult t
  }
}
/**
 * Adds support for String-based queries using OQL and continuous queries
 */
class GemfireStaticApi extends GormStaticApi {

  ContinuousQueryApi cqApi
  GemfireStaticApi(Class persistentClass, GemfireDatastore datastore) {
    super(persistentClass, datastore);
    cqApi = new ContinuousQueryApi(persistentEntity, datastore)
  }

  ContinuousQueryApi getCq() { cqApi }

  def executeFunction(Collection keys, Closure callable) {
    GemfireDatastore gemfire = datastore
    GemfireTemplate template = gemfire.getTemplate(persistentClass)
    def resultCollector = FunctionService
                            .onRegion(template.region)
                            .withFilter(keys as Set)
                            .execute(new ClosureInvokingFunction(callable))

    return resultCollector.getResult()
  }

  def executeFunction(Closure callable) {
    GemfireDatastore gemfire = datastore
    GemfireTemplate template = gemfire.getTemplate(persistentClass)
    def resultCollector = FunctionService
                            .onRegion(template.region)
                            .execute(new ClosureInvokingFunction(callable))

    return resultCollector.getResult()
  }
  
  def executeQuery(String query) {
    GemfireDatastore gemfire = datastore
    GemfireTemplate template = gemfire.getTemplate(persistentClass)

    template?.query(query)?.asList() ?: Collections.emptyList()
  }

  def executeQuery(String query, Collection params) {
    executeQuery(query, params, Collections.emptyMap())
  }

  def Object executeQuery(String query, Map args) {
    executeQuery(query, Collections.emptyList(), args)
  }

  def Object executeQuery(String query, Collection params, Map args) {
    GemfireDatastore gemfire = datastore
    GemfireTemplate template = gemfire.getTemplate(persistentClass)

    template.execute( { Region region ->
      def cache = gemfire.gemfireCache
      def queryService = cache.queryService

      def q = queryService.newQuery("SELECT DISTINCT * FROM /${persistentEntity.decapitalizedName} WHERE ${query}")
      def results = q.execute(params.toArray()).asList()

      if(args?.sort) {
        def prop = args.sort
        def ordering = new ManualEntityOrdering(persistentEntity)
        if(args?.order == 'desc')
          results = ordering.applyOrder(results, Query.Order.desc(prop))
        else
          results = ordering.applyOrder(results, Query.Order.asc(prop))
      }

      return results
    } as GemfireCallback)
  }

  def find(String query) {
    def results = executeQuery("$query LIMIT 1")
    if(results) {
      return results[0]
    }
    else {
      return null
    }
  }

  def find(String query, Collection params) {
    def results = executeQuery("$query LIMIT 1", params)
    if(results) {
      return results[0]
    }
    else {
      return null
    }
  }

  def find(String query, Map args) {
    def results = executeQuery("$query LIMIT 1", args)
    if(results) {
      return results[0]
    }
    else {
      return null
    }
  }

  def find(String query, Collection params, Map args) {
    def results = executeQuery("$query LIMIT 1", params, args)
    if(results) {
      return results[0]
    }
    else {
      return null
    }
  }

  List findAll(String query) {
    executeQuery(query)
  }

  List findAll(String query, Collection params) {
    executeQuery(query, params)
  }

  List findAll(String query, Collection params, Map args) {
    executeQuery(query, params, args)
  }


  List findAll(String query, Map args) {
    executeQuery(query, args)
  }




}
