package org.springframework.datastore.node;

import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.EntityAccess;
import org.springframework.datastore.engine.EntityPersister;
import org.springframework.datastore.mapping.ClassMapping;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.query.Query;

import java.io.Serializable;
import java.util.List;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class AbstractNodeEnityPerister<T> extends EntityPersister {
    protected Session session;
    protected ClassMapping classMapping;
    
    public AbstractNodeEnityPerister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity);
        this.session = session;
        this.classMapping = entity.getMapping();
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys) {
        return null;
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity persistentEntity, Iterable objs) {
        return null;
    }

    @Override
    protected Object retrieveEntity(PersistentEntity persistentEntity, Serializable key) {
        return null;
    }

    @Override
    protected Serializable persistEntity(PersistentEntity persistentEntity, EntityAccess entityAccess) {
        return null;
    }

    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {

    }

    @Override
    protected void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {
        
    }

    public Query createQuery() {
        return null;
    }
}
