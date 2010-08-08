package org.springframework.datastore.jcr.engine;

import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.keyvalue.engine.KeyValueEntry;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.query.Query;
import org.springmodules.jcr.JcrSessionFactory;
import org.springmodules.jcr.JcrTemplate;

import java.io.Serializable;
import java.util.List;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrEntityPersister extends AbstractKeyValueEntityPesister<KeyValueEntry, Object> {
    private JcrSessionFactory factory;
    private JcrTemplate jcrTemplate;

    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session) {
        super(context, entity, session);
    }

    public JcrEntityPersister(MappingContext context, PersistentEntity entity,Session session, JcrSessionFactory factory){
        super(context, entity, session);
        this.factory = factory;
        jcrTemplate = new JcrTemplate(factory);
    }


    @Override
    protected void deleteEntry(String family, Object key) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AssociationIndexer getAssociationIndexer(Association association) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected KeyValueEntry createNewEntry(String family) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Object getEntryValue(KeyValueEntry nativeEntry, String property) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void setEntryValue(KeyValueEntry nativeEntry, String key, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected KeyValueEntry retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, KeyValueEntry nativeEntry) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, Object key, KeyValueEntry entry) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Query createQuery() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
