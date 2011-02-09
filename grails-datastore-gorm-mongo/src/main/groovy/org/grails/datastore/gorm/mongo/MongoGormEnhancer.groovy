/* Copyright (C) 2010 SpringSource
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package org.grails.datastore.gorm.mongo

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.mongo.MongoDatastore
import org.springframework.datastore.mapping.mongo.MongoSession
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister
import org.springframework.transaction.PlatformTransactionManager

import com.gmongo.internal.DBCollectionPatcher
import com.mongodb.DBCollection
import com.mongodb.DBObject

/**
 * GORM enhancer for Mongo
 * 
 * @author Graeme Rocher
 *
 */
class MongoGormEnhancer extends GormEnhancer {
	
	public MongoGormEnhancer(Datastore datastore,
			PlatformTransactionManager transactionManager) {
		super(datastore, transactionManager);	
	}

	public MongoGormEnhancer(Datastore datastore) {
		super(datastore);
	}

	protected GormStaticApi getStaticApi(Class cls) {
		return new MongoGormStaticApi( cls, datastore )
	}
	
	protected GormInstanceApi getInstanceApi(Class cls) {
		return new MongoGormInstanceApi(cls, datastore)
	}
}

class MongoGormInstanceApi extends GormInstanceApi {

	public MongoGormInstanceApi(Class persistentClass, Datastore datastore) {
		super(persistentClass, datastore);
	}

	/**
	* Allows subscript access to schemaless attributes
	*
	* @param instance The instance
	* @param name The name of the field
	* @return
	*/
   void putAt(Object instance, String name, value) {
	   def dbo = getDbo(instance)
	   if(dbo != null) {
		   dbo.put name, value
	   }
   }
   
	/**
	 * Allows subscript access to schemaless attributes
	 * 
	 * @param instance The instance
	 * @param name The name of the field
	 * @return
	 */
	def getAt(Object instance, String name) {
		def dbo = getDbo(instance)
		if(dbo != null && dbo.containsField(name)) {
			return dbo.get(name)
		}
		return null	
	}
	
	/**
	 * Return the DBObject instance for the entity
	 * 
	 * @param instance The instance
	 * 
	 * @return The DBObject instance
	 */
	DBObject getDbo(Object instance) {
		MongoSession session = datastore.currentSession
		
		if(!session.contains(instance)) {
			if(!instance.save()) {
				throw new IllegalStateException("Cannot obtain DBObject for transient instance, save a valid instance first")
			}	
		}
		
		MongoEntityPersister persister = session.getPersister(instance)
		def id = persister.getObjectIdentifier(instance)
		
		return session.getCachedEntry( persister.getPersistentEntity(), id )
	}	
}
class MongoGormStaticApi extends GormStaticApi {

	public MongoGormStaticApi(Class persistentClass, Datastore datastore) {
		super(persistentClass, datastore);
	}
	
	/**
	 * @return The name of the Mongo collection that entity maps to
	 */
	String getCollectionName() {
		MongoDatastore ms = datastore
		def template = ms.getMongoTemplate(persistentEntity)

		template.getDefaultCollectionName()
	}
	
	/**
	 * The actual collection that this entity maps too
	 * 
	 * @return The actual collection
	 */
	DBCollection getCollection() {
		MongoDatastore ms = datastore
		def template = ms.getMongoTemplate(persistentEntity)

		
		def coll = template.getCollection(template.getDefaultCollectionName())
		DBCollectionPatcher.patch(coll)
		return coll
	}
}
