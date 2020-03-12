package org.grails.datastore.gorm

import grails.gorm.tests.NotInListSpec
import org.junit.platform.runner.JUnitPlatform
import org.junit.platform.suite.api.SelectClasses
import org.junit.runner.RunWith

/**
 * @author graemerocher
 */
@RunWith(JUnitPlatform)
@SelectClasses([NotInListSpec])
class SimpleMapTestSuite {
}
