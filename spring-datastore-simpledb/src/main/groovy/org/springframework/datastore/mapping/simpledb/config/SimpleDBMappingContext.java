package org.springframework.datastore.mapping.simpledb.config;

import org.springframework.datastore.mapping.document.config.Attribute;
import org.springframework.datastore.mapping.document.config.Collection;
import org.springframework.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy;

/**
 * Models a {@link org.springframework.datastore.mapping.model.MappingContext} for SimpleDB.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBMappingContext extends AbstractMappingContext {
    public SimpleDBMappingContext() {
        mappingFactory = createMappingFactory();
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }

    protected MappingFactory createMappingFactory() {
        return new GormKeyValueMappingFactory(null);
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new SimpleDBPersistentEntity(javaClass, this);
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    protected MappingConfigurationStrategy syntaxStrategy;
    MappingFactory<Collection, Attribute> mappingFactory;
}