package org.grails.datastore.gorm.mongo

import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.ProxyLoadingSpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.PagedResultSpec
import grails.gorm.tests.EnumSpec

/**
 * @author graemerocher
 */
@RunWith(Suite)
@SuiteClasses([
    ProxyLoadingSpec
])
class MongoSuite {
}