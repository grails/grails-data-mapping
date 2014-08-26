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
package org.grails.datastore.gorm.neo4j.bean.factory

import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.gorm.neo4j.HashcodeEqualsAwareProxyFactory
import org.grails.datastore.gorm.neo4j.Neo4jMappingContext
import org.grails.datastore.mapping.model.MappingContext

/**
 * Factory bean for construction the Neo4j DocumentMappingContext.
 *
 * @author Stefan Armbruster
 */
class Neo4jMappingContextFactoryBean extends AbstractMappingContextFactoryBean {

    protected MappingContext createMappingContext() {
        new Neo4jMappingContext(proxyFactory: new HashcodeEqualsAwareProxyFactory())
    }
}
