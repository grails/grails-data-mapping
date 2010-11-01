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
import org.junit.Test
import org.springframework.datastore.mapping.core.Session

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakPersisterTests extends GroovyTestCase {

  @Test
  void testEntityPersister() {
    RiakDatastore ds = new RiakDatastore()
    Session sess = ds.createSession(null)
    ds.getMappingContext().addPersistentEntity(TestEntity)

    // Save entity with random ID
    TestEntity t = new TestEntity()
    t.name = "test"
    t.lng = 1L
    t.intgr = 0

    //def key = sess.persist(t)
    //println "saved ${t} into ${key}"
    //sess.flush()

    //def t2 = sess.retrieve(TestEntity, key)
    //println "retrieved ${t2}"

    // Save entity with specific ID
    TestEntity t3 = new TestEntity()
    t3.id = "thisisatest"
    t3.name = "test3"
    t3.lng = 2L
    t3.intgr = 1
    t3.children = [new TestEntity(id: "childtest1", name: "child1"), new TestEntity(id: "childtest2", name: "child2")]

    //def key3 = sess.persist(t3)
    //println "saved ${t3} into ${key3}"
    //sess.flush()

    def t4 = sess.retrieve(TestEntity, "thisisatest")
    println "loaded ${t4}"
    println "children: ${t4.children}"

  }
}

@Entity
class TestEntity {

  static hasMany = [children: TestEntity]

  String id
  String name
  Long lng
  Integer intgr
  List<TestEntity> children
}
