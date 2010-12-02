/*
 * Copyright (c) 2010 by NPC International, Inc.
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

import org.grails.datastore.gorm.riak.RiakGormEnhancer
import org.springframework.data.riak.core.RiakTemplate
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.riak.RiakDatastore
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class Setup {

  static riak

  static Session setup(classes) {
    riak = new RiakDatastore(new KeyValueMappingContext(""), [
        defaultUri: "http://localhost:8098/riak/{bucket}/{key}",
        mapReduceUri: "http://localhost:8098/mapred"
    ])
    for (cls in classes) {
      riak.mappingContext.addPersistentEntity(cls)
    }

    PersistentEntity entity = riak.mappingContext.persistentEntities.find { PersistentEntity e ->
      e.name.contains("TestEntity")
    }

    riak.mappingContext.addEntityValidator(entity, [
        supports: { Class c -> true },
        validate: { Object o, Errors errors ->
          if (!StringUtils.hasText(o.name)) {
            errors.rejectValue("name", "name.is.blank")
          }
        }
    ] as Validator)

    def enhancer = new RiakGormEnhancer(riak, new DatastoreTransactionManager(datastore: riak))
    enhancer.enhance()

    riak.mappingContext.addMappingContextListener({ e ->
      enhancer.enhance e
    } as MappingContext.Listener)


    Session con = riak.connect()
    RiakTemplate riakTmpl = con.nativeInterface
    riakTmpl.useCache = false
    [
        "grails.gorm.tests.TestEntity",
        "grails.gorm.tests.ChildEntity",
        "grails.gorm.tests.Publication",
        "grails.gorm.tests.Location",
        "grails.gorm.tests.City",
        "grails.gorm.tests.Country",
        "grails.gorm.tests.Highway",
        "grails.gorm.tests.Book",
        "grails.gorm.tests.Pet",
        "grails.gorm.tests.Person"
    ].each { type ->
      def schema = riakTmpl.getBucketSchema(type, true)
      riakTmpl.updateBucketSchema(type, [n_val: 3, w: 3, dw: 3])
      riakTmpl.updateBucketSchema(type + ".metadata", [n_val: 3, w: 3, dw: 3])
      schema.keys.each { key ->
        try {
          riakTmpl.deleteKeys("$type:$key")
        } catch (err) {
        }
      }
    }

    return con

  }
}
