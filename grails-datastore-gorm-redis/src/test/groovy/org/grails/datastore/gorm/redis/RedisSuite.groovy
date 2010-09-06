package org.grails.datastore.gorm.redis

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.FindByMethodSpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.GroovyProxySpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.NegationSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 9:19:54 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
      NegationSpec
])
class RedisSuite {
}
