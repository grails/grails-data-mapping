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


package org.springframework.datastore.mapping.mongo.config;

import org.springframework.datastore.mapping.config.AbstractGormMappingFactory;
import org.springframework.datastore.mapping.document.config.Attribute;
import org.springframework.datastore.mapping.document.config.DocumentMappingContext;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.MappingFactory;

/**
 * Models a {@link MappingContext} for Mongo
 * 
 * @author Graeme Rocher
 *
 */

public class MongoMappingContext extends DocumentMappingContext {

	public MongoMappingContext(String defaultDatabaseName) {
		super(defaultDatabaseName);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected MappingFactory createDocumentMappingFactory() {
		return new  AbstractGormMappingFactory<MongoCollection, Attribute>() {
			@Override
			protected Class<Attribute> getPropertyMappedFormType() {
				return Attribute.class;
			}

			@Override
			protected Class<MongoCollection> getEntityMappedFormType() {
				return MongoCollection.class;
			}
		};
	}


}
