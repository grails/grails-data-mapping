package org.grails.datastore.mapping.node.mapping;

import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeMappingFactory extends MappingFactory {

    @Override
    public Node createMappedForm(PersistentEntity entity) {
        return new Node(entity.getName());
    }

    @Override
    public NodeProperty createMappedForm(PersistentProperty mpp) {
        return new NodeProperty(mpp.getName());
    }
}
