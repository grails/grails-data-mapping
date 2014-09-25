package grails.gorm.tests

import grails.persistence.Entity

@Entity
class ConfigEntity {
	String lastName
	String firstName    
    String location   
	Integer age
	
	static mapping = {
		id name:"lastName", primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned", column: "surname"
		firstName index:true, primaryKey:[ordinal:1, type: "clustered"], order: "desc"
        location index:true, primaryKey:[ordinal:2, type: "clustered"], type:'ascii', order: "desc"
        age index:true, column: "person_age"  
		sort location: "desc"      		
	}	
	
	static tableOptions = {		
		compact_storage true
		comment "table comment"
		compaction class: "LeveledCompactionStrategy", sstable_size_in_mb: 300, tombstone_compaction_interval: 2
		compression sstable_compression: "LZ4Compressor", chunk_length_kb: 128,	crc_check_chance: 0.75
		caching keys : "all"
		bloom_filter_fp_chance 0.2
		read_repair_chance 0.1
		dclocal_read_repair_chance 0.2
		gc_grace_seconds 900000
	}
}
