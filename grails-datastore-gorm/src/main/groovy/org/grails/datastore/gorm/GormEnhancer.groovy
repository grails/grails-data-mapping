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

/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
class GormEnhancer {

  Datastore datastore


  GormEnhancer(datastore) {
    this.datastore = datastore;
  }

  void enhance() {
    for(PersistentEntity e in datastore.mappingContext.persistentEntities) {
      enhance e.javaClass
    }
  }
  void enhance(Class cls) {
    def staticMethods = new GormStaticApi(cls,datastore)
    def instanceMethods = new GormInstanceApi(datastore)
    cls.metaClass {
      save {-> instanceMethods.save(delegate) }
      delete {-> instanceMethods.delete(delegate) }
      'static' {
        list staticMethods.&list
        get staticMethods.&get
        exists staticMethods.&exists
        withSession staticMethods.&withSession
      }
    }
  }
}
