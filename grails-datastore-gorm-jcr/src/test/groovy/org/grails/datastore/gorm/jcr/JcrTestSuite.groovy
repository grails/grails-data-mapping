package org.grails.datastore.gorm.jcr

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.GormEnhancerSpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.FindByMethodSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.QueryAfterPropertyChangeSpec
import grails.gorm.tests.GroovyProxySpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.CommonTypesPersistenceSpec
import grails.gorm.tests.CircularOneToManySpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.OrderBySpec
import grails.gorm.tests.ValidationSpec
import grails.gorm.tests.UpdateWithProxyPresentSpec
import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.NegationSpec
import grails.gorm.tests.RangeQuerySpec

/**
 * Created by IntelliJ IDEA.
 * User: Erawat
 * Date: 29-Oct-2010
 * Time: 01:46:23
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
//  DomainEventsSpec, - cannot be tested
//  ProxyLoadingSpec, // passed
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec, // StackOverflow
//  InheritanceSpec,
//  FindByMethodSpec,
//  ListOrderBySpec,
//  GroovyProxySpec, // passed
//  CommonTypesPersistenceSpec, // passed
//  GormEnhancerSpec,
//  CriteriaBuilderSpec,
//  NegationSpec,
//  NamedQuerySpec,
//  OrderBySpec,
//  RangeQuerySpec,
//  ValidationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec, // passed
//  WithTransactionSpec,
//  CrudOperationsSpec // passed
])
class JcrTestSuite {
}
