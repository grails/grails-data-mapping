package org.grails.datastore.gorm.mongo

import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.document.config.Attribute;
import org.springframework.datastore.mapping.document.config.DocumentPersistentEntity;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.mongo.MongoSession;
import org.springframework.datastore.mapping.mongo.config.MongoCollection 

import com.mongodb.WriteConcern;

import grails.gorm.tests.GormDatastoreSpec;

class MongoEntityConfigSpec extends GormDatastoreSpec{
	
	def "Test custom collection config"() {
		given:
			session.mappingContext.addPersistentEntity MyMongoEntity
			
		when:
			PersistentEntity entity = session.mappingContext.getPersistentEntity(MyMongoEntity.name)
			
		then:
			entity instanceof DocumentPersistentEntity
			
		when:	
			MongoCollection coll = entity.mapping.mappedForm
			Attribute attr = entity.getPropertyByName("name").getMapping().getMappedForm()
		then:
			coll != null
			coll.collection == 'mycollection'
			coll.database = "mydb"
			coll.writeConcern == WriteConcern.FSYNC_SAFE
			attr != null
			attr.index == true
			attr.targetName == 'myattribute'
			
		when:
			def t = new MyMongoEntity(name:"Bob").save(flush:true)
			MongoSession ms = session
			MongoTemplate mt = ms.getMongoTemplate(entity)
		then:
			t != null
			mt.getDefaultCollectionName() == "mycollection"
			
			
	}

}
class MyMongoEntity {
	String id
	
	String name
	
	static mapping = {
		collection "mycollection"
		database "mydb"
		shard "name"
		writeConcern WriteConcern.FSYNC_SAFE
		name index:true, attr:"myattribute"
	}
}
