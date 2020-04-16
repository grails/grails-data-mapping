package org.grails.datastore.gorm

import grails.gorm.tests.NotInListSpec
import org.junit.platform.runner.JUnitPlatform
import org.junit.platform.suite.api.SelectClasses
import org.junit.runner.RunWith

/**
 * @author graemerocher
 */
//TODO: Replace with JUnit5 declarative test suites once https://github.com/junit-team/junit5/issues/744 is resolved
@RunWith(JUnitPlatform)
@SelectClasses([NotInListSpec])
class SimpleMapTestSuite {
}
