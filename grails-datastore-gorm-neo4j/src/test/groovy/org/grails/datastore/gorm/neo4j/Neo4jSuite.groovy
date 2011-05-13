package org.grails.datastore.gorm.neo4j

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.*

@RunWith(Suite)
@SuiteClasses([
//FindOrSaveWhereSpec,
//FindOrCreateWhereSpec,
//OptimisticLockingSpec,
//WithTransactionSpec,

  OneToManySpec,
  CircularOneToManySpec,
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
  CrudOperationsSpec,

  TraverserSpec
])
class Neo4jSuite {
}
