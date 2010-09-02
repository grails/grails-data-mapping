package org.springframework.datastore.node.mapping;

import org.springframework.datastore.mapping.AbstractPersistentEntity;
import org.springframework.datastore.mapping.MappingContext;

/**
 * TODO: write javadoc
 * 
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodePersistentEntity extends AbstractPersistentEntity {

    public NodePersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
    }
}
