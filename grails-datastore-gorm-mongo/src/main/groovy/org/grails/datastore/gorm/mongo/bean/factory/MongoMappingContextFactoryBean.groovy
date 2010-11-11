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

import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.mongo.config.MongoMappingContext 
import org.springframework.util.Assert;

/**
 * Factor bean for construction the Mongo DocumentMappingContext
 * 
 * @author Graeme Rocher
 *
 */
class MongoMappingContextFactoryBean extends AbstractMappingContextFactoryBean{

	
	String defaultDatabaseName
	
	@Override
	protected MappingContext createMappingContext() {
	
		Assert.hasText(defaultDatabaseName, "Property [defaultDatabaseName] must be set!")
		
		return new MongoMappingContext(defaultDatabaseName);
	}

}
