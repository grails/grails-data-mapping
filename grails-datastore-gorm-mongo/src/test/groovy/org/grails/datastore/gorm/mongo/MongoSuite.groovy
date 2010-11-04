package org.grails.datastore.gorm.mongo

import grails.gorm.tests.CrudOperationsSpec
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.junit.runner.RunWith

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 03/11/2010
 * Time: 09:58
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
//  GormEnhancerSpec,
//  CriteriaBuilderSpec,
//  NegationSpec,
//  NamedQuerySpec,
//  OrderBySpec,
//  RangeQuerySpec,
//  ValidationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec,
  CrudOperationsSpec
])
class MongoSuite {
}