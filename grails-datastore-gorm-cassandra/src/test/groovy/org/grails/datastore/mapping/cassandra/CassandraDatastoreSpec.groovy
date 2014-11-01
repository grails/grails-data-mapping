package org.grails.datastore.mapping.cassandra

import org.grails.datastore.gorm.cassandra.mapping.MappingCassandraConverter
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.cassandra.config.KeyspaceAction
import org.springframework.cassandra.config.KeyspaceActionSpecificationFactoryBean
import org.springframework.cassandra.core.keyspace.KeyspaceOption.ReplicationStrategy
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
		config.put(CassandraDatastore.KEYSPACE_CONFIG, keyspaceConfig)
	}

	private createCassandraDatastoreWithMocks(ConfigObject config) {					
		def keyspace = config.get(CassandraDatastore.KEYSPACE_CONFIG)?.get(CassandraDatastore.KEYSPACE_NAME)
		CassandraDatastore cassandraDatastore = new CassandraDatastore(new CassandraMappingContext(keyspace), config, null)
		cassandraDatastore.setCassandraCqlClusterFactoryBean(clusterBean)
		cassandraDatastore.setKeyspaceActionSpecificationFactoryBean(keyspaceBean)
		cassandraDatastore.setCassandraSessionFactoryBean(sessionBean)
		
		def mockCluster = Mock(Cluster)
		1 * clusterBean.getObject() >> mockCluster
		1 * sessionBean.getObject() >> Mock(Session)
		1 * clusterBean.afterPropertiesSet()
		1 * sessionBean.setCluster(mockCluster)
		1 * sessionBean.afterPropertiesSet()
		
		cassandraDatastore
	}	
	
	void "Test default configure CassandraDatastore"() {
		given:
    		def keyspace = "default"    	
			config = new ConfigSlurper().parse('''
				grails.cassandra.keyspace.name="default"
			''')?.grails.cassandra			    	
    		CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
			
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
	
	void "Test create keyspace actions"() {
		given:
			def keyspace = "newkeyspace"
			
		when:
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, keyspace)
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "create")
			CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * keyspaceBean.setName(keyspace)
			1 * keyspaceBean.setAction(KeyspaceAction.CREATE)
		
		when:
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "create-drop")
			cassandraDatastore = createCassandraDatastoreWithMocks(config)
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * keyspaceBean.setName(keyspace)
			1 * keyspaceBean.setAction(KeyspaceAction.CREATE_DROP)
	}
	
	void "Test create keyspace with options"() {
		given:
			def keyspace = "newkeyspace"
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, keyspace)
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "create-drop")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_DURABLE_WRITES, "false")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_REPLICATION_STRATEGY, "SimpleStrategy")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_REPLICATION_FACTOR, 2)
			CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
		
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
		
	void "Test create keyspace, custom contact points and port, no durable writes" () {	
		given: 
			def keyspace = "newkeyspace"	
			config = new ConfigSlurper().parse('''
				grails {
                	cassandra {
                		contactPoints = "192.168.0.10"
						port = "5555"
                		dbCreate="recreate-drop-unused"		
                		keyspace {
                			name = "newkeyspace"
                			action="create"                			
                			durableWrites = false                			
                		}
                	}
                }
			''')?.grails.cassandra			    	
			CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
			
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
	
	void "Test create keyspace network topology strategy"() {
		given:
			def keyspace = "newkeyspace"
			config = new ConfigSlurper().parse('''
				grails {
                	cassandra {
                		contactPoints = "192.168.0.10, 192.168.0.11"
						port = 1234
                		dbCreate="create"		
                		keyspace {
                			name = "newkeyspace"
                			action="create"                			
                			durableWrites = false
                			replicationStrategy = "NetworkTopologyStrategy"
                			dataCenter = ["us-west":1, "eu-west":2]
                		}
                	}
                }
			''')?.grails.cassandra
    		    		
			CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
		
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
	
	void "Test invalid keyspace action"() {
		given:
			def keyspace = "default"
								
		when:
        	config = new ConfigSlurper().parse('''
        				grails.cassandra.keyspace.name="default"
        				grails.cassandra.keyspace.action="Invalid"
        			''')?.grails.cassandra			    	
        	CassandraDatastore cassandraDatastore = new CassandraDatastore(new CassandraMappingContext(keyspace), config, null)
			cassandraDatastore.afterPropertiesSet()

		then:
			def e = thrown(IllegalArgumentException)
			e.message.startsWith("Invalid option [Invalid] for property [action], allowable values are")
		
		when:
			config = new ConfigSlurper().parse('''
				grails.cassandra.keyspace.name="default"
				grails.cassandra.keyspace.action=[1]
			''')?.grails.cassandra			    	
			cassandraDatastore = new CassandraDatastore(new CassandraMappingContext(keyspace), config, null)				
			cassandraDatastore.afterPropertiesSet()

		then:
			e = thrown(IllegalArgumentException)
			e.message.startsWith("Invalid type for property [[1]], expected java.lang.String")
	}
	
	
	void "Test create schema options"() {
		when:
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, "newkeyspace")
			CassandraDatastore cassandraDatastore = createCassandraDatastoreWithMocks(config)
			cassandraDatastore.afterPropertiesSet()
		then:
			1 * sessionBean.setSchemaAction(SchemaAction.NONE)
				
		when:			
			config.put(CassandraDatastore.SCHEMA_ACTION, "none")	    	
			cassandraDatastore = createCassandraDatastoreWithMocks(config)    		
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * sessionBean.setSchemaAction(SchemaAction.NONE)
		
		when:
			config.put(CassandraDatastore.SCHEMA_ACTION, "create")	
			cassandraDatastore = createCassandraDatastoreWithMocks(config)    		
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * sessionBean.setSchemaAction(SchemaAction.CREATE)
		
		when:
			config.put(CassandraDatastore.SCHEMA_ACTION, "recreate")
			cassandraDatastore = createCassandraDatastoreWithMocks(config)
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * sessionBean.setSchemaAction(SchemaAction.RECREATE)
		
		when:
			config.put(CassandraDatastore.SCHEMA_ACTION, "recreate-drop-unused")
			cassandraDatastore = createCassandraDatastoreWithMocks(config)
			cassandraDatastore.afterPropertiesSet()
		
		then:
			1 * sessionBean.setSchemaAction(SchemaAction.RECREATE_DROP_UNUSED)
		
		when:
			config = new ConfigSlurper().parse('''grails.cassandra.dbCreate="recreate-invalid-unused"''')?.grails.cassandra			    	
			cassandraDatastore = new CassandraDatastore(new CassandraMappingContext("new"), config, null)    									
			cassandraDatastore.afterPropertiesSet()
		
		then:
			def e = thrown(IllegalArgumentException)
			e.message.startsWith("Invalid option [recreate-invalid-unused] for property [dbCreate], allowable values are")
	}
}