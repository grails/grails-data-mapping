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

import org.grails.datastore.gorm.neo4j.converters.StringToBigDecimalConverter
import org.grails.datastore.gorm.neo4j.converters.StringToBigIntegerConverter
import org.grails.datastore.gorm.neo4j.converters.StringToCurrencyConverter
import org.grails.datastore.gorm.neo4j.converters.StringToLocaleConverter
import org.grails.datastore.gorm.neo4j.converters.StringToShortConverter
import org.grails.datastore.gorm.neo4j.converters.StringToTimeZoneConverter
import org.grails.datastore.gorm.neo4j.converters.StringToURLConverter
import org.grails.datastore.mapping.document.config.Attribute
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.model.MappingConfigurationStrategy
import org.grails.datastore.mapping.model.MappingFactory
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy
import org.grails.datastore.gorm.neo4j.converters.IntArrayToIntegerArrayConverter
import org.grails.datastore.gorm.neo4j.converters.LongArrayToLongArrayConverter

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jMappingContext extends AbstractMappingContext {

    MappingFactory<Collection, Attribute> mappingFactory
    MappingConfigurationStrategy syntaxStrategy

    Neo4jMappingContext() {
        mappingFactory = new GraphGormMappingFactory()
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)
        //addTypeConverter(new StringToNumberConverterFactory().getConverter(BigDecimal))
        addTypeConverter(new StringToShortConverter())
        addTypeConverter(new StringToBigIntegerConverter())
        addTypeConverter(new StringToBigDecimalConverter())
        addTypeConverter(new StringToCurrencyConverter())
        addTypeConverter(new StringToLocaleConverter())
        addTypeConverter(new StringToTimeZoneConverter())
        addTypeConverter(new StringToURLConverter())
        addTypeConverter(new IntArrayToIntegerArrayConverter())
        addTypeConverter(new LongArrayToLongArrayConverter())
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        GraphPersistentEntity persistentEntity = new GraphPersistentEntity(javaClass, this)
        mappingFactory.createMappedForm(persistentEntity) // populates mappingFactory.entityToPropertyMap as a side effect
        persistentEntity
    }

    MappingConfigurationStrategy getMappingSyntaxStrategy() {
        syntaxStrategy
    }

    MappingFactory getMappingFactory() {
        mappingFactory
    }
}
