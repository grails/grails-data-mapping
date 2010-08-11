package org.springframework.datastore.node;

import org.springframework.datastore.mapping.AbstractMappingContext;
import org.springframework.datastore.mapping.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.MappingFactory;
import org.springframework.datastore.mapping.PersistentEntity;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrMappingContext extends AbstractMappingContext {

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return null;  //TODO.
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return null;  //TODO.
    }

    public MappingFactory getMappingFactory() {
        return null;  //TODO.
    }
}
