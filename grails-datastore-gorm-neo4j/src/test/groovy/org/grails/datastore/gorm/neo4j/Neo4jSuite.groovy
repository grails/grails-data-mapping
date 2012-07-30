package org.grails.datastore.gorm.neo4j

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.*

@RunWith(Suite)
@SuiteClasses([

AttachMethodSpec,
CircularOneToManySpec,
CommonTypesPersistenceSpec,
CriteriaBuilderSpec,
CrudOperationsSpec,
DetachedCriteriaSpec,
DomainEventsSpec,
EnumSpec,
FindByExampleSpec,
FindByMethodSpec,
FindOrCreateWhereSpec,
FindOrSaveWhereSpec,
FindWhereSpec,
GormEnhancerSpec,
GroovyProxySpec,
InheritanceSpec,
ListOrderBySpec,
NamedQuerySpec,
NegationSpec,
OneToManySpec,
OneToOneSpec,
OptimisticLockingSpec,
OrderBySpec,
PagedResultSpec,
PropertyComparisonQuerySpec,
ProxyLoadingSpec,
QueryAfterPropertyChangeSpec,
QueryByAssociationSpec,
RangeQuerySpec,
SaveAllSpec,
SizeQuerySpec,
UpdateWithProxyPresentSpec,
ValidationSpec,
WithTransactionSpec,

ApiExtensionsSpec,
ManyToManySpec,
MiscSpec,
JoinCriteriaSpec

])
class Neo4jSuite {
}
