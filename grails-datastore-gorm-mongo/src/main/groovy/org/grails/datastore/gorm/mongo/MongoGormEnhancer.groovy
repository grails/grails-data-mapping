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

import org.grails.datastore.gorm.GormEnhancer;
import org.grails.datastore.gorm.GormStaticApi 
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.mongo.MongoDatastore;
import org.springframework.transaction.PlatformTransactionManager;

import com.mongodb.DBCollection;

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

		template.getCollection(template.getDefaultCollectionName())
	}
}
