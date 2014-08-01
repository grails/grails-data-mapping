package org.grails.datastore.gorm.mongo

import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.DeleteAllSpec
import grails.gorm.tests.DetachedCriteriaSpec
import grails.gorm.tests.FindWhereSpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.SizeQuerySpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.PagedResultSpec
import grails.gorm.tests.EnumSpec
import grails.gorm.tests.OrderBySpec
import grails.gorm.tests.OneToManySpec

/**
 * @author graemerocher
 */
@RunWith(Suite)
@SuiteClasses([
    DetachedCriteriaSpec
])
class MongoSuite {
}
