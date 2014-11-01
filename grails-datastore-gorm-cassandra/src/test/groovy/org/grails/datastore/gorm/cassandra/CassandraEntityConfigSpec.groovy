

package org.grails.datastore.gorm.cassandra

import grails.gorm.tests.ConfigEntity
import grails.gorm.tests.TestEntity
import grails.persistence.Entity

import org.grails.datastore.gorm.Setup
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.grails.datastore.mapping.cassandra.config.CassandraPersistentEntity
import org.grails.datastore.mapping.cassandra.config.Column
import org.grails.datastore.mapping.cassandra.config.Table
import org.grails.datastore.mapping.model.IllegalMappingException
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query.Order.Direction
import org.springframework.cassandra.core.Ordering

import spock.lang.Specification

import com.datastax.driver.core.TableMetadata
import com.datastax.driver.core.TableMetadata.Options

class CassandraEntityConfigSpec extends Specification{
	
	def keyspace = "configtest"
	CassandraMappingContext cassandraMappingContext = new CassandraMappingContext(keyspace)
	ConfigObject config = new ConfigObject()
	ConfigObject keyspaceConfig = new ConfigObject()
	
	def setup() {
		config.put(CassandraDatastore.KEYSPACE_CONFIG, keyspaceConfig)
	}
	
	def "Test entity config simple primary key"() {
		given:
			cassandraMappingContext.addPersistentEntity(TestEntity)
		
		when:
			PersistentEntity entity = cassandraMappingContext.getPersistentEntity(TestEntity.name)
		
		then:
			entity instanceof CassandraPersistentEntity
		
		when:
			Table table = entity.mapping.mappedForm
			Column id = entity.getIdentity().getMapping().getMappedForm()
			
		then:
			table != null
			table.keyspace == keyspace
			!table.hasCompositePrimaryKeys()
			table.primaryKeys == []
			table.primaryKeyNames == []
			!table.isPrimaryKey("id")
			!table.sort
			!table.tableProperties
			
			id != null	
			!id.name					
			!id.primaryKeyAttributes
			!id.index
			!id.order
			!id.type
	}
	
	def "Test entity config composite primary key"() {
		given:
			cassandraMappingContext.addPersistentEntity(ConfigEntity)

		when:
			PersistentEntity entity = cassandraMappingContext.getPersistentEntity(ConfigEntity.name)
			Table table = entity.mapping.mappedForm
			Column lastName = entity.getIdentity().getMapping().getMappedForm()
			Column firstName = entity.getPropertyByName("firstName").getMapping().getMappedForm()
			Column location = entity.getPropertyByName("location").getMapping().getMappedForm()
			Column age = entity.getPropertyByName("age").getMapping().getMappedForm()
						
		then:
			table != null
			table.keyspace == keyspace		
			table.hasCompositePrimaryKeys()
			table.primaryKeys == [lastName, firstName, location]
			table.primaryKeyNames == ["lastName", "firstName", "location"]
			table.isPrimaryKey("lastName")
			table.isPrimaryKey("firstName")
			table.isPrimaryKey("location")
			table.sort.property == "location"
			table.sort.direction == Direction.DESC								
			
			lastName != null
			lastName.name == "lastName"
			lastName.isPartitionKey()
			lastName.primaryKeyAttributes
			lastName.primaryKeyAttributes.ordinal == 0
			lastName.primaryKeyAttributes.type == "partitioned"
			lastName.generator == "assigned"
			!lastName.index
			
			firstName != null
			firstName.name == "firstName"
			firstName.isClusterKey()
			firstName.primaryKeyAttributes.ordinal == 1
			firstName.primaryKeyAttributes.type == "clustered"			
			firstName.index		
			firstName.order == Ordering.DESCENDING	
			
			location != null
			location.name == "location"
			location.isClusterKey()
			location.primaryKeyAttributes.ordinal == 2
			location.primaryKeyAttributes.type == "clustered"
			location.type == "ascii"
			location.index		
			location.order == Ordering.DESCENDING	
			
			age != null
			age.name == "age"
			!age.primaryKeyAttributes
			age.primaryKeyAttributes == null
			age.index
			age.order == null		
	}
	
	def "Test entity table options"() {
		given:
			cassandraMappingContext.addPersistentEntity(ConfigEntity)
				
		when:		
			PersistentEntity entity = cassandraMappingContext.getPersistentEntity(ConfigEntity.name)	
											
		then:
			Table table = entity.mapping.mappedForm
    		table != null    		    	
			Map tableOptions = table.tableProperties
			tableOptions.size() == 9
			tableOptions.comment == "table comment"
			tableOptions.compact_storage
			tableOptions.compaction == [class: "LeveledCompactionStrategy", sstable_size_in_mb: 300, tombstone_compaction_interval: 2]
			tableOptions.compression == [sstable_compression: "LZ4Compressor", chunk_length_kb: 128, crc_check_chance: 0.75]
			tableOptions.caching == "all"
			tableOptions.bloom_filter_fp_chance == 0.2
			tableOptions.read_repair_chance == 0.1
			tableOptions.dclocal_read_repair_chance == 0.2
			tableOptions.gc_grace_seconds == 900000
	}		
	
	def "Test entity table options check Cassandra metadata"() {
		given:
			def keyspace = Setup.randomKeyspaceName()    
			config.put(CassandraDatastore.SCHEMA_ACTION, "recreate-drop-unused")		
    		keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, keyspace)
    		keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "create-drop")
			cassandraMappingContext = new CassandraMappingContext(keyspace)
			cassandraMappingContext.addPersistentEntity(TablePropertiesEntity)
    		CassandraDatastore cassandraDatastore = new CassandraDatastore(cassandraMappingContext, config, null)    
			
		when:
			cassandraDatastore.afterPropertiesSet()		
		
		then:
    		def cluster = cassandraDatastore.nativeCluster
    		cluster != null
			TableMetadata tableMetadata = cluster.metadata.getKeyspace(keyspace).getTable("tablepropertiesentity")
			tableMetadata != null
			
			Options options = tableMetadata.options
			options.comment == "table comment"
			options.isCompactStorage
			options.caching == "ALL"
			options.bloomFilterFalsePositiveChance == 0.2
			options.readRepairChance == 0.1
			options.localReadRepairChance == 0.2
			options.gcGraceInSeconds == 900000
			
			Map compaction = options.compaction
			compaction.class.endsWith("SizeTieredCompactionStrategy")
			compaction.bucket_high == "2.5"
			compaction.bucket_low == "0.6"
			compaction.max_threshold == "40"
			compaction.min_threshold == "5"
			compaction.min_sstable_size == "60"
			
			Map compression = options.compression
			compression.crc_check_chance == "0.85"
			compression.sstable_compression.endsWith("LZ4Compressor")
			compression.chunk_length_kb == "128"
					
		cleanup:
			cassandraDatastore?.destroy()
			
	}		
	
	def "Test invalid entity table options"() {
		given:
			def keyspace = Setup.randomKeyspaceName()
			config.put(CassandraDatastore.SCHEMA_ACTION, "recreate-drop-unused")
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_NAME, keyspace)
			keyspaceConfig.put(CassandraDatastore.KEYSPACE_ACTION, "create-drop")
			cassandraMappingContext = new CassandraMappingContext(keyspace)
			cassandraMappingContext.addPersistentEntity(TablePropertiesEntity)
			PersistentEntity entity = cassandraMappingContext.getPersistentEntity(TablePropertiesEntity.name)
			Table table = entity.mapping.mappedForm
			Map originalOptions = table.tableProperties
			
		when: "Invalid table option"
			CassandraDatastore cassandraDatastore = new CassandraDatastore(cassandraMappingContext, config, null)			
			table.tableProperties = [comment: "comment", invalidOption : true]
			cassandraDatastore.afterPropertiesSet()
										
		then:
			def e = thrown(IllegalMappingException)			
			e.message.startsWith("Invalid option [invalidOption] for [tableOptions], allowable values are")			
		
		when: "Invalid compaction option"
			cassandraDatastore = new CassandraDatastore(cassandraMappingContext, config, null)			
			table.tableProperties = [compaction: [class: "LeveledCompactionStrategy", unknown: 300]]
			cassandraDatastore.afterPropertiesSet()
										
		then:
			e = thrown(IllegalMappingException)
			e.message.startsWith("Invalid option [unknown] for [compaction options], allowable values are")			
		
		when: "Invalid column marked as ordered"
    		cassandraDatastore = new CassandraDatastore(cassandraMappingContext, config, null)
			table.tableProperties = originalOptions    		
			Column name = entity.getPropertyByName("firstName").getMapping().getMappedForm()		
			name.setPrimaryKey(null)	
			table.addColumn(name)
			cassandraDatastore.afterPropertiesSet()
		
		then:
			e = thrown(IllegalMappingException)
			e.message == "Invalid mapping for property [firstName]. [order] attribute can only be set for a clustered primary key"
			
		cleanup:
			cassandraDatastore?.destroy()
	}
		
}

@Entity
class TablePropertiesEntity {
	
	String lastName
    String firstName
	
	static mapping = {
		id name:"lastName", primaryKey:[ordinal:0, type:"partitioned"]
		firstName primaryKey:[ordinal:1, type: "clustered"], order:"desc"	
	}
	
	static tableProperties = {
		comment "table comment"
		"COMPACT STORAGE" true
		replicate_on_write false
		caching "all"
		bloom_filter_fp_chance 0.2
		read_repair_chance 0.1
		dclocal_read_repair_chance 0.2
		gc_grace_seconds 900000
		compaction class: "SizeTieredCompactionStrategy", bucket_high: 2.5, bucket_low: 0.6, max_threshold: 40, min_threshold: 5, min_sstable_size: 60
		compression sstable_compression: "LZ4Compressor", chunk_length_kb: 128,	crc_check_chance: 0.85
		
	}
}