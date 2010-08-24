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
 * Time: 1:00:25 PM
 * To change this template use File | Settings | File Templates.
 */
class RangeQueryTests extends grails.gorm.tests.RangeQueryTests{
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
