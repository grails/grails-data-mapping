package org.grails.datastore.gorm.jcr

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.GormEnhancerSpec

/**
 * Created by IntelliJ IDEA.
 * User: Erawat
 * Date: 29-Oct-2010
 * Time: 01:46:23
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
//  DomainEventsSpec,
//  ProxyLoadingSpec,
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec,
//  InheritanceSpec,
//  FindByMethodSpec,
//  ListOrderBySpec,
//  GroovyProxySpec,
//  CommonTypesPersistenceSpec,
 GormEnhancerSpec,
//  CriteriaBuilderSpec,
//  NegationSpec,
//  NamedQuerySpec,
//  OrderBySpec,
//  RangeQuerySpec,
//  ValidationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec,
//  CrudOperationsSpec - Done
])
class JcrTestSuite {
}
