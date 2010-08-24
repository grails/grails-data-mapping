package org.grails.datastore.test

import org.junit.After
import org.grails.datastore.gorm.GormEnhancer
import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity
import org.springframework.datastore.mock.SimpleMapDatastore
import org.junit.Before
import org.springframework.datastore.core.Session

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 24, 2010
 * Time: 10:29:28 AM
 * To change this template use File | Settings | File Templates.
 */
class GormEnhancerTests extends grails.gorm.tests.GormEnhancerTests{
  Session con
  @Before
  void setup() {
    def datastore = new SimpleMapDatastore()
    datastore.mappingContext.addPersistentEntity(TestEntity)
    datastore.mappingContext.addPersistentEntity(ChildEntity)
    new GormEnhancer(datastore).enhance()
    con = datastore.connect()
  }

  @After
  void disconnect() {
    con.disconnect()
  }  
}
