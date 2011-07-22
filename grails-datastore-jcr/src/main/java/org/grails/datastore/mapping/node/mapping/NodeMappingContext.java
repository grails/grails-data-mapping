package org.grails.datastore.mapping.node.mapping;

import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeMappingContext extends AbstractMappingContext {
    private MappingConfigurationStrategy syntaxStrategy;
    private NodeMappingFactory mappingFactory;

    public NodeMappingContext() {
        this.mappingFactory = new NodeMappingFactory();
        this.syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new NodePersistentEntity(javaClass, this);
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }
}
