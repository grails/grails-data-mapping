package org.springframework.datastore.mapping.node.mapping;

import org.springframework.datastore.mapping.model.AbstractMappingContext;
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy;

/**
 * TODO: write javadoc
 * 
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeMappingContext extends AbstractMappingContext {
    private MappingConfigurationStrategy syntaxStrategy;
    private NodeMappingFactory mappingFactory;
   
    public NodeMappingContext(){
        this.mappingFactory = new NodeMappingFactory();
        this.syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new NodePersistentEntity(javaClass, this);
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return  this.syntaxStrategy;  
    }

    public MappingFactory getMappingFactory() {
        return this.mappingFactory;
    }
}
