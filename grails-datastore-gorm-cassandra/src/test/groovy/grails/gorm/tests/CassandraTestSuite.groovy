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
//CircularOneToManySpec,
//CommonTypesPersistenceSpec,
//ConstraintsSpec,
//CriteriaBuilderSpec,
CrudOperationsSpec,
DeleteAllSpec,
//DetachedCriteriaSpec,
//DomainEventsSpec,
//EnumSpec,
//FindByExampleSpec,
FindByMethodSpec,
FindOrCreateWhereSpec,
FindOrSaveWhereSpec,
//FirstAndLastMethodSpec,
GormEnhancerSpec,
//GroovyProxySpec,
//InheritanceSpec,
ListOrderBySpec,
//NamedQuerySpec,
//NegationSpec,
//OneToManySpec,
//OneToOneSpec,
//OptimisticLockingSpec,
OrderBySpec,
//PagedResultSpec,
PersistenceEventListenerSpec,
//PropertyComparisonQuerySpec,
//ProxyLoadingSpec,
QueryAfterPropertyChangeSpec,
//QueryByAssociationSpec,
QueryByNullSpec,
QueryEventsSpec,
//RangeQuerySpec,
SaveAllSpec,
SessionCreationEventSpec,
//SizeQuerySpec,
//UniqueConstraintSpec,
//UpdateWithProxyPresentSpec,
//ValidationSpec,
//WithTransactionSpec
])
class CassandraTestSuite {
}
