package org.springframework.datastore.node.mapping;

import org.springframework.datastore.mapping.MappingFactory;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;

/**
 *
 * TODO: write javadoc
 * 
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeMappingFactory extends MappingFactory{
    @Override
    public Node createMappedForm(PersistentEntity entity) {
        return new Node(entity.getName());
    }

    @Override
    public NodeProperty createMappedForm(PersistentProperty mpp) {
        return new NodeProperty(mpp.getName()); 
    }
}
