package org.grails.datastore.gorm

import grails.gorm.tests.NamedQuerySpec
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.junit.runner.RunWith
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.NegationSpec
import grails.gorm.tests.UpdateWithProxyPresentSpec
import grails.gorm.tests.QueryAfterPropertyChangeSpec
import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.GroovyProxySpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.FindByMethodSpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.CircularOneToManySpec
import grails.gorm.tests.ProxyLoadingSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 1:16:26 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
//  ProxyLoadingSpec,
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec,
//  InheritanceSpec,
//  FindByMethodSpec,
//  ListOrderBySpec,
//  GroovyProxySpec,
  DomainEventsSpec,
//  CriteriaBuilderSpec,
//  NegationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec
])
class SimpleMapTestSuite {
}
