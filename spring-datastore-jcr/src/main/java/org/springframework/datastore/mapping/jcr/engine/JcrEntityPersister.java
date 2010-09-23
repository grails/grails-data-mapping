package org.springframework.datastore.mapping.jcr.engine;

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jcr.JcrSession;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.node.engine.AbstractNodeEntityPersister;
import org.springframework.extensions.jcr.JcrTemplate;

import javax.jcr.Node;
import java.io.Serializable;


/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrEntityPersister extends AbstractNodeEntityPersister<Node, String> {
    
    public JcrEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
    }

    public JcrEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, JcrTemplate jcrTemplate) {
        super(mappingContext, entity, session);
    }

  
}

