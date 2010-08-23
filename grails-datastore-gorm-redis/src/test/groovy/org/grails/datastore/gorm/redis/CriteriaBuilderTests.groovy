package org.grails.datastore.gorm.redis

import org.junit.After
import org.junit.Before
import org.springframework.datastore.core.Session
import static org.grails.datastore.gorm.redis.RedisSetup.setup



/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 12:26:09 PM
 * To change this template use File | Settings | File Templates.
 */
class CriteriaBuilderTests extends grails.gorm.tests.CriteriaBuilderTests{
  Session con
  @Before
  void setupRedis() {
    con = setup()
  }

  @After
  void disconnect() {
    con.disconnect()
  }
}
