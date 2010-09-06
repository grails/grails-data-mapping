package org.grails.datastore.gorm

import grails.gorm.tests.NamedQuerySpec
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.junit.runner.RunWith
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.NegationSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 1:16:26 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
      NegationSpec
])

class SimpleMapTestSuite {
}
