package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.model.AbstractMappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy
import org.springframework.datastore.mapping.model.MappingFactory
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy
import org.springframework.datastore.mapping.document.config.Attribute
import org.springframework.datastore.mapping.document.config.GormDocumentMappingFactory
import org.grails.datastore.gorm.neo4j.converters.StringToCurrencyConverter
import org.grails.datastore.gorm.neo4j.converters.StringToLocaleConverter
import org.grails.datastore.gorm.neo4j.converters.StringToTimeZoneConverter
import org.grails.datastore.gorm.neo4j.converters.StringToURLConverter
import org.grails.datastore.gorm.neo4j.converters.StringToBigDecimalConverter
import org.grails.datastore.gorm.neo4j.converters.StringToBigIntegerConverter
import org.grails.datastore.gorm.neo4j.converters.StringToShortConverter

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 17:24
 * To change this template use File | Settings | File Templates.
 */
class Neo4jMappingContext extends AbstractMappingContext {

    MappingFactory<Collection, Attribute> mappingFactory;
    MappingConfigurationStrategy syntaxStrategy

    Neo4jMappingContext() {
        mappingFactory = new GormDocumentMappingFactory() //new GraphGormMappingFactory()
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)
        //addTypeConverter(new StringToNumberConverterFactory().getConverter(BigDecimal))
        addTypeConverter(new StringToShortConverter())
        addTypeConverter(new StringToBigIntegerConverter())
        addTypeConverter(new StringToBigDecimalConverter())
        addTypeConverter(new StringToCurrencyConverter())
        addTypeConverter(new StringToLocaleConverter())
        addTypeConverter(new StringToTimeZoneConverter())
        addTypeConverter(new StringToURLConverter())
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new GraphPersistentEntity(javaClass, this)
    }

    MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy
    }

    MappingFactory getMappingFactory() {
        mappingFactory
    }
}
