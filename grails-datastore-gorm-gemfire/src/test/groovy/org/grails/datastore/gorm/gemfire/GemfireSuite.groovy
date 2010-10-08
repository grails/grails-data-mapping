package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.*
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.junit.runner.RunWith

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 5, 2010
 * Time: 3:34:07 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Suite)
@SuiteClasses([
  NamedQuerySpec,
//  CriteriaBuilderSpec,
//  OrderBySpec,
//  CommonTypesPersistenceSpec,
//  QueryAfterPropertyChangeSpec,
//  QueryByAssociationSpec,
//  UpdateWithProxyPresentSpec,
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
//  NegationSpec,
//  RangeQuerySpec,
//  ValidationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec,
  CrudOperationsSpec
])
class GemfireSuite {
}
