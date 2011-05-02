package org.grails.datastore.gorm.neo4j

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.QueryAfterPropertyChangeSpec
import grails.gorm.tests.CircularOneToManySpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.FindByMethodSpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.GroovyProxySpec
import grails.gorm.tests.CommonTypesPersistenceSpec
import grails.gorm.tests.GormEnhancerSpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.NegationSpec
import grails.gorm.tests.NamedQuerySpec
import grails.gorm.tests.OrderBySpec
import grails.gorm.tests.RangeQuerySpec
import grails.gorm.tests.ValidationSpec
import grails.gorm.tests.UpdateWithProxyPresentSpec
import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.WithTransactionSpec

@RunWith(Suite)
@SuiteClasses([
//  CircularOneToManySpec,
//  WithTransactionSpec,

  InheritanceSpec,
  UpdateWithProxyPresentSpec,
  ListOrderBySpec,
  CriteriaBuilderSpec,
  OrderBySpec,
  RangeQuerySpec,
  NamedQuerySpec,
  NegationSpec,
  GormEnhancerSpec,
  ValidationSpec,
  FindByMethodSpec,
  QueryAfterPropertyChangeSpec,
  AttachMethodSpec,
  CommonTypesPersistenceSpec,
  GroovyProxySpec,
  ProxyLoadingSpec,
  DomainEventsSpec,
  CrudOperationsSpec

])
class Neo4jSuite {
}
