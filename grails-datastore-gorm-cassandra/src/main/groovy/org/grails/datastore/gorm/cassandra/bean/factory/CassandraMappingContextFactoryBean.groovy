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
package org.grails.datastore.gorm.cassandra.bean.factory

import groovy.transform.Canonical

import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.util.Assert

/**
 * Factory bean for construction the Cassandra MappingContext.
 *
 */
class CassandraMappingContextFactoryBean extends AbstractMappingContextFactoryBean {
	String keyspace
	DefaultMappingHolder defaultMapping
	
    @Override
    protected MappingContext createMappingContext() {
		Assert.hasText(keyspace, "Property [keyspace] must be set!")
        new CassandraMappingContext(keyspace, defaultMapping?.defaultMapping)        
    }
}

@Canonical
class DefaultMappingHolder {
	Closure defaultMapping
}
