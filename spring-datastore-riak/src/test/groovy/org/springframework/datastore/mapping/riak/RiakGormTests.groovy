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

package org.springframework.datastore.mapping.riak

import grails.persistence.Entity
import org.grails.datastore.gorm.riak.RiakGormEnhancer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakGormTests extends GroovyTestCase {

  static Logger log = LoggerFactory.getLogger(RiakGormTests)

  RiakDatastore riak
  RiakGormEnhancer enhancer
  RiakSession session
  def ids

  protected void setUp() {
    if (!riak) {
      riak = new RiakDatastore(new KeyValueMappingContext(""), ["host": "127.0.0.1"])
      riak.mappingContext.addPersistentEntity(GormTestEntity)

      enhancer = new RiakGormEnhancer(riak, new DatastoreTransactionManager(datastore: riak))
      enhancer.enhance()

      riak.mappingContext.addMappingContextListener({ e ->
        enhancer.enhance(e)
      } as MappingContext.Listener)

      session = riak.connect()
    }

    ids = []
    def lastNames = ["Blow", "Somebody", "Loser", "Schmoe", "Lewis"]
    [1, 2, 3, 4, 5].each {
      GormTestEntity e = new GormTestEntity()
      e.id = it
      e.name = "Joe ${lastNames[it - 1]}"
      session.persist(e)
      session.flush()
      log.debug "Persisted: $e"
      ids << e.id
    }
  }

  void testFindByName() {
    def e = GormTestEntity.findByName("Joe Somebody")
    log.debug "Joe1: $e"
  }

  void testCriteria() {
    def crit = GormTestEntity.createCriteria()
    def results = crit.list {
      like('name', 'Joe %')
    }
    log.debug "Criteria results: $results"
    assertEquals 5, results.size()
  }

  void testMapReduce() {
    def results = GormTestEntity.mapReduce.map("""function(v){
      var o=Riak.mapValuesJson(v);
      ejsLog('/tmp/mapred.log', JSON.stringify(o));
      return [v.key];
    }""")
    log.debug "M/R results: $results"
  }

  protected void tearDown() {
    ids.each {
      log.debug "Deleting entity: $it"
      session.delete(GormTestEntity.get(it))
      session.flush()
    }
  }

}

@Entity
class GormTestEntity {
  Long id
  String name

  def String toString() {
    return "{id=$id, name=\"$name\"}"
  }


}