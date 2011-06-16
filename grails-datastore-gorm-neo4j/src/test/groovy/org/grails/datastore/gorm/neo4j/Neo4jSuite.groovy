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
DomainEventsSpec,
FindByExampleSpec,
FindByMethodSpec,
FindOrCreateWhereSpec,
FindOrSaveWhereSpec,
GormEnhancerSpec,
GroovyProxySpec,
InheritanceSpec,
ListOrderBySpec,
NamedQuerySpec,
NegationSpec,
OneToManySpec,
//OptimisticLockingSpec,
OrderBySpec,
ProxyLoadingSpec,
QueryAfterPropertyChangeSpec,
QueryByAssociationSpec,
RangeQuerySpec,
SaveAllSpec,
UpdateWithProxyPresentSpec,
ValidationSpec,
WithTransactionSpec,

ApiExtensionsSpec

])
class Neo4jSuite {
}
