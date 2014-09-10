package org.grails.datastore.mapping.cassandra

import org.grails.datastore.gorm.cassandra.mapping.MappingCassandraConverter
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.cassandra.config.KeyspaceAction
import org.springframework.cassandra.config.KeyspaceActionSpecificationFactoryBean
import org.springframework.cassandra.core.keyspace.KeyspaceOption.ReplicationStrategy
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.cassandra.config.SchemaAction

import spock.lang.Specification

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session


class CassandraDatastoreSpec extends Specification {
	
	CassandraCqlClusterFactoryBean clusterBean = Mock()
	KeyspaceActionSpecificationFactoryBean keyspaceBean = Mock()
	GormCassandraSessionFactoryBean sessionBean = Mock()
	ConfigObject config = new ConfigObject()
	ConfigObject keyspaceConfig = new ConfigObject()
	
	def	setup() { 						
		def mockCluster = Mock(Cluster)
		1 * clusterBean.getObject() >> mockCluster		
		1 * sessionBean.getObject() >> Mock(Session)		
		1 * clusterBean.afterPropertiesSet()		
		1 * sessionBean.setCluster(mockCluster)		
		1 * sessionBean.afterPropertiesSet()
	}

	private createCassandraDatastore(ConfigObject config) {
		def ctx = new GenericApplicationContext()
		ctx.refresh()
		def keyspace = config.get(CassandraDatastore.KEYSPACE_CONFIG)?.get(CassandraDatastore.KEYSPACE_NAME)
		CassandraDatastore cassandraDatastore = new CassandraDatastore(new CassandraMappingContext(keyspace), config, ctx)
		cassandraDatastore.setCassandraCqlClusterFactoryBean(clusterBean)
		cassandraDatastore.setKeyspaceActionSpecificationFactoryBean(keyspaceBean)
		cassandraDatastore.setCassandraSessionFactoryBean(sessionBean)
		cassandraDatastore
	}	
	
	void "Test default configure CassandraDatastore"() {
		given:
    		def keyspace = "default"    	
			config = new ConfigSlurper().parse('''
				grails.cassandra.keyspace.name="default"
			''')?.grails.cassandra			    	
    		CassandraDatastore cassandraDatastore = createCassandraDatastore(config)
			
		when:
			cassandraDatastore.afterPropertiesSet()

		then:"Then the Spring Data Cassandra beans have their properties set with defaults"
    		1 * clusterBean.setContactPoints(CassandraCqlClusterFactoryBean.DEFAULT_CONTACT_POINTS)
    		1 * clusterBean.setPort(CassandraCqlClusterFactoryBean.DEFAULT_PORT)
    		1 * clusterBean.setKeyspaceSpecifications(Collections.emptySet())
    		    	
			0 * keyspaceBean.setName(keyspace)
			
			1 * sessionBean.setKeyspaceName(keyspace)
			1 * sessionBean.setConverter(_ as MappingCassandraConverter)
			1 * sessionBean.setSchemaAction(SchemaAction.NONE)
			
    		cassandraDatastore.nativeCluster != null
    		cassandraDatastore.nativeSession != null
    		cassandraDatastore.cassandraTemplate != null
    		cassandraDatastore.cassandraAdminTemplate != null
	}
	
	void "Test create new keyspace, custom contact points and port, no durable writes" () {	
		given: 
			def keyspace = "newkeyspace"	
			config = new ConfigSlurper().parse('''
				grails {
                	cassandra {
                		contactPoints = "192.168.0.10"
						port = "5555"
                		schemaAction="RECREATE_DROP_UNUSED"		
                		keyspace {
                			name = "newkeyspace"
                			action="CREATE"                			
                			durableWrites = false                			
                		}
                	}
                }
			''')?.grails.cassandra			    	
			CassandraDatastore cassandraDatastore = createCassandraDatastore(config)
			
		when:
			cassandraDatastore.afterPropertiesSet()
			
		then:
			1 * clusterBean.setContactPoints(config.get(CassandraDatastore.CONTACT_POINTS))
    		1 * clusterBean.setPort(Integer.parseInt(config.get(CassandraDatastore.PORT)))
			1 * clusterBean.setKeyspaceSpecifications(_ as Set)
			
			1 * keyspaceBean.setName(keyspace)
			1 * keyspaceBean.setAction(KeyspaceAction.CREATE)
			1 * keyspaceBean.setReplicationStrategy(ReplicationStrategy.SIMPLE_STRATEGY)
			1 * keyspaceBean.setReplicationFactor(1)
			1 * keyspaceBean.setDurableWrites(false)
			1 * keyspaceBean.setIfNotExists(true)
			1 * keyspaceBean.afterPropertiesSet()
			1 * keyspaceBean.getObject() >> new HashSet()
			
			1 * sessionBean.setKeyspaceName(keyspace)	
			1 * sessionBean.setSchemaAction(SchemaAction.RECREATE_DROP_UNUSED)
			
	}
	
	void "Test custom keyspace"() {
		given:
    		def keyspace = "newkeyspace"			    	
			config.put(CassandraDatastore.KEYSPACE_CONFIG, keyspaceConfig)
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, keyspace)
    		keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "CREATE_DROP")
    		keyspaceConfig.put(CassandraDatastore.KEYSPACE_DURABLE_WRITES, "false")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_REPLICATION_STRATEGY, "SimpleStrategy")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_REPLICATION_FACTOR, 2)
			CassandraDatastore cassandraDatastore = createCassandraDatastore(config)
		
		when:
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * clusterBean.setKeyspaceSpecifications(_ as Set)
		
    		1 * keyspaceBean.setName(keyspace)
    		1 * keyspaceBean.setAction(KeyspaceAction.CREATE_DROP)
    		1 * keyspaceBean.setReplicationStrategy(ReplicationStrategy.SIMPLE_STRATEGY)
    		1 * keyspaceBean.setReplicationFactor(2)
    		1 * keyspaceBean.setDurableWrites(false)
    		1 * keyspaceBean.setIfNotExists(true)
    		1 * keyspaceBean.afterPropertiesSet()
    		1 * keyspaceBean.getObject() >> new HashSet()
	}
	
	void "Test network topology strategy"() {
		given:
			def keyspace = "newkeyspace"
			config = new ConfigSlurper().parse('''
				grails {
                	cassandra {
                		contactPoints = "192.168.0.10, 192.168.0.11"
						port = 1234
                		schemaAction="CREATE"		
                		keyspace {
                			name = "newkeyspace"
                			action="CREATE"                			
                			durableWrites = false
                			replicationStrategy = "NetworkTopologyStrategy"
                			dataCenter = ["us-west":1, "eu-west":2]
                		}
                	}
                }
			''')?.grails.cassandra
    		    		
			CassandraDatastore cassandraDatastore = createCassandraDatastore(config)
		
		when:
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * clusterBean.setContactPoints(config.get(CassandraDatastore.CONTACT_POINTS))
			1 * clusterBean.setPort(config.get(CassandraDatastore.PORT))
			1 * clusterBean.setKeyspaceSpecifications(_ as Set)
		
    		1 * keyspaceBean.setName(keyspace)
    		1 * keyspaceBean.setAction(KeyspaceAction.CREATE)
    		1 * keyspaceBean.setReplicationStrategy(ReplicationStrategy.NETWORK_TOPOLOGY_STRATEGY)
    		0 * keyspaceBean.setReplicationFactor(1)
    		1 * keyspaceBean.setDurableWrites(false)
    		1 * keyspaceBean.setIfNotExists(true)
    		1 * keyspaceBean.afterPropertiesSet()
    		1 * keyspaceBean.getObject() >> new HashSet()
			
			1 * sessionBean.setKeyspaceName(keyspace)
			1 * sessionBean.setConverter(_ as MappingCassandraConverter)
			1 * sessionBean.setSchemaAction(SchemaAction.CREATE)
	}
}