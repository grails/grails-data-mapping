package grails.gorm.tests

import org.grails.datastore.mapping.cassandra.BasicPersistenceCompositeKeySpec
import org.grails.datastore.mapping.cassandra.BasicPersistenceSpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

/**
 * @author Erawat
 */
@RunWith(Suite)
@SuiteClasses([
AttachMethodSpec,
BasicPersistenceSpec,
BasicPersistenceCompositeKeySpec,
CircularOneToManySpec,
CollectionTypesPersistenceSpec,
CommonTypesPersistenceSpec,
ConstraintsSpec,
CriteriaBuilderSpec,
CrudOperationsSpec,
DeleteAllSpec,
DetachedCriteriaSpec,
DisableAutotimeStampSpec, 
DomainEventsSpec,
EnumSpec,
FindByExampleSpec,
FindByMethodSpec,
FindOrCreateWhereSpec,
FindOrSaveWhereSpec,
FirstAndLastMethodSpec,
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
PersistenceEventListenerSpec,
PropertyComparisonQuerySpec,
ProxyLoadingSpec,
QueryAfterPropertyChangeSpec,
QueryByAssociationSpec,
QueryByNullSpec,
QueryEventsSpec,
RangeQuerySpec,
SaveAllSpec,
SessionCreationEventSpec,
SessionPropertiesSpec,
SizeQuerySpec,
UniqueConstraintSpec,
UpdateWithProxyPresentSpec,
ValidationSpec,
WithTransactionSpec
])
class CassandraTestSuite {
}
