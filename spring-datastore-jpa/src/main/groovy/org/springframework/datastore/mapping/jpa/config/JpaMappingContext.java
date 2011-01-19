/* Copyright (C) 2011 SpringSource
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


package org.springframework.datastore.mapping.jpa.config;

import javax.persistence.Column;
import javax.persistence.Table;

import org.springframework.datastore.mapping.model.AbstractMappingContext;
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;

/**
 * A MappingContext for JPA compatible entities
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaMappingContext extends AbstractMappingContext{

	private MappingFactory<Table, Column> mappingFactory = new JpaMappingFactory();
	private MappingConfigurationStrategy jpaMappingSyntaxStrategy = new JpaMappingConfigurationStrategy(mappingFactory);

	@Override
	public MappingConfigurationStrategy getMappingSyntaxStrategy() {
		return jpaMappingSyntaxStrategy ;
	}

	@Override
	public MappingFactory getMappingFactory() {
		return mappingFactory;
	}

	@Override
	protected PersistentEntity createPersistentEntity(Class javaClass) {
		return new JpaPersistentEntity(javaClass, this);
	}

}
