package org.grails.datastore.gorm.redis

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.FindByMethodSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 9:19:54 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
      FindByMethodSpec
])
class RedisSuite {
}
