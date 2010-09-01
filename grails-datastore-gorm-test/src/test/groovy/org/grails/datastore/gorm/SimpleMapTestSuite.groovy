package org.grails.datastore.gorm

import grails.gorm.tests.NamedQuerySpec
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.junit.runner.RunWith
import grails.gorm.tests.WithTransactionSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 1:16:26 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
        WithTransactionSpec
])

class SimpleMapTestSuite {
}
