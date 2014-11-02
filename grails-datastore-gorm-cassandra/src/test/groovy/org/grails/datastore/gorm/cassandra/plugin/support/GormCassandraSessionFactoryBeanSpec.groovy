package org.grails.datastore.gorm.cassandra.plugin.support

import grails.gorm.tests.ConfigEntity
import grails.gorm.tests.PersonLastNamePartitionKey
import grails.gorm.tests.Simples
import grails.gorm.tests.TestEntity

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext
import org.grails.datastore.gorm.cassandra.mapping.MappingCassandraConverter
import org.grails.datastore.mapping.cassandra.GormCassandraSessionFactoryBean
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.springframework.cassandra.core.cql.generator.CreateTableCqlGenerator
import org.springframework.data.cassandra.core.CassandraAdminTemplate

import spock.lang.Specification

import com.datastax.driver.core.Session

class GormCassandraSessionFactoryBeanSpec extends Specification {

	def keyspace = "configtest"
	CassandraMappingContext cassandraMappingContext = new CassandraMappingContext(keyspace)
	BasicCassandraMappingContext springCassandraMappingContext = new BasicCassandraMappingContext(cassandraMappingContext)	
	GormCassandraSessionFactoryBean gormCassandraSessionFactoryBean = new GormCassandraSessionFactoryBean(cassandraMappingContext, springCassandraMappingContext)		
	
	void "Test entity config simple primary key"() {
		given:
    		cassandraMappingContext.addPersistentEntity(TestEntity)
    		springCassandraMappingContext.getPersistentEntity(TestEntity)
		
    	when :
    		List createTableSpecifications = gormCassandraSessionFactoryBean.createTableSpecifications(springCassandraMappingContext.getNonPrimaryKeyEntities())
    						
    	then:
    		createTableSpecifications.size() == 1
    		def cql = new CreateTableCqlGenerator(createTableSpecifications.get(0)).toCql()
    		cql == "CREATE TABLE testentity (id uuid, age int, name text, version bigint, PRIMARY KEY (id));"
	}	
	
	void "Test entity config composite primary key"() {
		given:
    		cassandraMappingContext.addPersistentEntity(PersonLastNamePartitionKey)
    		def entity = springCassandraMappingContext.getPersistentEntity(PersonLastNamePartitionKey)
		
    	when :
    		def createTableSpecification = gormCassandraSessionFactoryBean.createTableSpecification(entity)
    						
    	then:    		
    		def cql = new CreateTableCqlGenerator(createTableSpecification).toCql()
    		cql == "CREATE TABLE personlastnamepartitionkey (surname text, firstname text, person_age int, location text, PRIMARY KEY (surname, firstname, person_age));"
	}
	
	void "Test entity config table options CQL"() {
		given: 
			cassandraMappingContext.addPersistentEntity(ConfigEntity)
			def entity = springCassandraMappingContext.getPersistentEntity(ConfigEntity)
			
		when:
			def createTableSpecification = gormCassandraSessionFactoryBean.createTableSpecification(entity)
							
		then:			
			def cql = new CreateTableCqlGenerator(createTableSpecification).toCql()
			cql == "CREATE TABLE configentity (surname text, firstname text, location ascii, person_age int, PRIMARY KEY (surname, firstname, location)) " + 
				   "WITH CLUSTERING ORDER BY (firstname DESC, location DESC) " +
				   "AND COMPACT STORAGE AND comment = 'table comment' " +
				   "AND compaction = { 'class' : 'LeveledCompactionStrategy', 'sstable_size_in_mb' : 300, 'tombstone_compaction_interval' : 2 } " +
				   "AND compression = { 'sstable_compression' : 'LZ4Compressor', 'chunk_length_kb' : 128, 'crc_check_chance' : 0.75 } " +
				   "AND caching = 'all' AND bloom_filter_fp_chance = 0.2 AND read_repair_chance = 0.1 AND dclocal_read_repair_chance = 0.2 AND gc_grace_seconds = 900000;"
	}	
	
	void "Test createTable"() {
		given:
			cassandraMappingContext.addPersistentEntity(Simples)
			def entity = springCassandraMappingContext.getPersistentEntity(Simples)
			Session session = Mock()
			gormCassandraSessionFactoryBean.admin = new CassandraAdminTemplate(session, new MappingCassandraConverter(springCassandraMappingContext))
			
		when:
			gormCassandraSessionFactoryBean.createTable(Simples)
		
		then:
			1 * session.execute("CREATE TABLE simples (id uuid, name text, version bigint, PRIMARY KEY (id));")		
			1 * session.execute("CREATE INDEX IF NOT EXISTS  ON simples (name);")
			0 * session._
	}
}
