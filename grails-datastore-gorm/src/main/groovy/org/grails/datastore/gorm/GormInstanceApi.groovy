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

/**
 * Instance methods of the GORM API
 *
 * @author Graeme Rocher
 */
class GormInstanceApi extends AbstractGormApi {

  GormInstanceApi(Class persistentClass, Datastore datastore) {
    super(persistentClass, datastore)
  }

  def save(instance) {
    datastore.currentSession.persist(instance)
    return instance
  }

  def save(instance, Map params) {
    datastore.currentSession.persist(instance)
    return instance    
  }

  def delete(instance) {
    datastore.currentSession.delete(instance)
  }

}
