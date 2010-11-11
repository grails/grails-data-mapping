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

package org.grails.datastore.gorm.mongo.bean.factory

import org.grails.datastore.gorm.events.AutoTimestampInterceptor 
import org.grails.datastore.gorm.events.DomainEventInterceptor 
import org.springframework.beans.factory.FactoryBean;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.mongo.MongoDatastore;

import com.mongodb.Mongo;

/**
 * Factory bean for constructing a {@link MongoDatastore} instance.
 * 
 * @author Graeme Rocher
 *
 */
class MongoDatastoreFactoryBean implements FactoryBean<MongoDatastore>{

	Mongo mongo
	MappingContext mappingContext
	Map<String,String> config = [:]

	@Override
	public MongoDatastore getObject() throws Exception {
		
		MongoDatastore datastore
		if(mongo != null)
		 	datastore = new MongoDatastore(mappingContext, mongo,config)
		else {
			datastore = new MongoDatastore(mappingContext, config)
		}
		
		datastore.addEntityInterceptor(new DomainEventInterceptor())
		datastore.addEntityInterceptor(new AutoTimestampInterceptor())
		datastore.afterPropertiesSet()
		return datastore;
	}

	@Override
	public Class<?> getObjectType() { MongoDatastore }

	@Override
	boolean isSingleton() { true }

}
