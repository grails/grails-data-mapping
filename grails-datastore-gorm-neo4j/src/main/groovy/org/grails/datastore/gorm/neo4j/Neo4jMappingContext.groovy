package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.model.AbstractMappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy
import org.springframework.datastore.mapping.model.MappingFactory
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy
import org.springframework.datastore.mapping.document.config.Attribute
import org.springframework.datastore.mapping.document.config.GormDocumentMappingFactory

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
