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
package org.grails.datastore.gorm.cassandra

import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.grails.datastore.mapping.cassandra.CassandraMappingContext
import org.springframework.beans.factory.config.MethodInvokingFactoryBean


/**
 * Cassandra specific configuration logic for Spring
 *
 */
class CassandraSpringConfigurer extends SpringConfigurer {
	@Override
	String getDatastoreType() {
		return "Cassandra"
	}

	@Override
	Closure getSpringCustomizer() {
		return {
			def cassandraConfig = application.config?.grails?.cassandra?.clone()

			cassandraMappingContext(CassandraMappingContextFactoryBean) {
				grailsApplication = ref('grailsApplication')
				pluginManager = ref('pluginManager')
				config = cassandraConfig
			}



			cassandraDatastore(CassandraDatastoreFactoryBean) {
				mappingContext = cassandraMappingContext
				config = cassandraConfig
			}
		}
	}
}
