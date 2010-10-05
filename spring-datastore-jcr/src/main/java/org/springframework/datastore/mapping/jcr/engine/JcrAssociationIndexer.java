package org.springframework.datastore.mapping.jcr.engine;

import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.model.PersistentEntity;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrAssociationIndexer implements AssociationIndexer<Serializable, Serializable> {
    public void index(Serializable primaryKey, List<Serializable> foreignKeys) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Serializable> query(Serializable primaryKey) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PersistentEntity getIndexedEntity() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void index(Serializable primaryKey, Serializable foreignKey) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
