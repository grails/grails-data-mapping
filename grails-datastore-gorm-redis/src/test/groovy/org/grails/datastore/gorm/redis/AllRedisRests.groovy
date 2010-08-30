package org.grails.datastore.gorm.redis

import grails.gorm.tests.AllTests
import org.junit.runner.RunWith
import org.junit.runners.model.RunnerBuilder
import org.junit.runner.Runner
import grails.gorm.tests.TestEntity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 10:26:28 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(RedisRunner)
class AllRedisRests extends AllTests{

  static class RedisRunner extends AllTests.TestSuite {

    RedisRunner(Class klass, RunnerBuilder builder) {
      super(klass, builder);
    }

    def session
    protected void tearDown() {
      session?.disconnect()
    }

    protected void setUp() {
      session = RedisSetup.setup()
    }

  }
}
