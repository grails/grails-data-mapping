package org.grails.datastore.gorm

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.*

/**
 * @author graemerocher
 */
@RunWith(Suite)
@SuiteClasses([
ValidationSpec
])
class SimpleMapTestSuite {
}
