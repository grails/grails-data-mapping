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
import org.springframework.datastore.query.Query
import grails.gorm.CriteriaBuilder
import org.grails.datastore.gorm.finders.DynamicFinder

/**
 *  Static methods of the GORM API
 *
 * @author Graeme Rocher
 */
class GormStaticApi extends AbstractGormApi {


  GormStaticApi(Class persistentClass, Datastore datastore) {
    super(persistentClass,datastore)
  }

  /**
   * Retrieves and object from the datastore. eg. Book.get(1)
   */
  def get(Serializable id) {
    datastore.currentSession.retrieve(persistentClass,id)
  }

  /**
   * Creates a criteria builder instance
   */
  CriteriaBuilder createCriteria() {
    return new CriteriaBuilder(persistentClass, datastore)
  }

  /**
   * Creates a criteria builder instance
   */
  CriteriaBuilder withCriteria(Closure callable) {
    return new CriteriaBuilder(persistentClass, datastore).list(callable)
  }

  /**
   * Locks an instance for an update
   * @param id The identifier
   * @return The instance
   */
  def lock(Serializable id) {
    datastore.currentSession.lock(persistentClass, id)
  }

  /**
   * Counts the number of persisted entities
   * @return The number of persisted entities
   */
  Integer count() {
    def q = datastore.currentSession.createQuery(persistentClass)
    q.projections().count()
    q.singleResult() as Integer
  }

  /**
   * Checks whether an entity exists
   */
  boolean exists(Serializable id) {
    get(id) != null
  }

  /**
   * Lists objects in the datastore. eg. Book.list(max:10)
   *
   * @param params Any parameters such as offset, max etc.
   * @return A list of results
   */
  List list(Map params) {
    Query q = datastore.currentSession.createQuery(persistentClass)
    DynamicFinder.populateArgumentsForCriteria(persistentClass, q, params)
    q.list()
  }

  def withSession(Closure callable) {
    callable.call(datastore.currentSession)
  }

  private safeInt(params, name, defaultValue) {
    def value = params?.get(name)
    if(value) {
      try {
        return Integer.parseInt(value)
      } catch (NumberFormatException e) {
      }
    }
    return defaultValue
  }
}
