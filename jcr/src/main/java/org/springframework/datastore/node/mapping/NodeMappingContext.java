package org.springframework.datastore.node.mapping;

import org.springframework.datastore.mapping.AbstractMappingContext;
import org.springframework.datastore.mapping.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.MappingFactory;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.config.GormMappingConfigurationStrategy;

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
