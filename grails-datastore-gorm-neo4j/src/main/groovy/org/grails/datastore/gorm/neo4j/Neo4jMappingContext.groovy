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
package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.converters.*
import org.grails.datastore.mapping.document.config.Attribute
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.model.MappingConfigurationStrategy
import org.grails.datastore.mapping.model.MappingFactory
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy
/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
class Neo4jMappingContext extends AbstractMappingContext  {

    MappingFactory<Collection, Attribute> mappingFactory = new GraphGormMappingFactory()
    MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)

//    Neo4jMappingContext() {
//        mappingFactory = new GraphGormMappingFactory()
//        syntaxStrategy =
//    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
         PersistentEntity persistentEntity = new GraphPersistentEntity(javaClass, this)
//        mappingFactory.createMappedForm(persistentEntity) // populates mappingFactory.entityToPropertyMap as a side effect
        persistentEntity
    }

//    MappingFactory getMappingFactory() {
//        mappingFactory
//    }
}
