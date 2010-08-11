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

/**
 *  Static methods of the GORM API
 *
 * @author Graeme Rocher
 */
class GormStaticApi {

  Class persistentClass
  Datastore datastore

  GormStaticApi(Class persistentClass, Datastore datastore) {
    this.persistentClass = persistentClass;
    this.datastore = datastore
  }

  /**
   * Retrieves and object from the datastore. eg. Book.get(1)
   */
  def get(Serializable id) {
    datastore.currentSession.retrieve(persistentClass,id)
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
    q.offset safeInt(params, "offset", 0)
    def max = safeInt(params, "max", -1)
    if(max)
      q.max max

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
