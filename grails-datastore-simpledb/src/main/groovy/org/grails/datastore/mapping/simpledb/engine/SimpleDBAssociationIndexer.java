package org.grails.datastore.mapping.simpledb.engine;

import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.simpledb.SimpleDBSession;

import java.util.Collections;
import java.util.List;

/**
 * An {@link org.grails.datastore.mapping.engine.AssociationIndexer} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBAssociationIndexer implements AssociationIndexer {
    private SimpleDBNativeItem nativeEntry;
    private Association association;
    private SimpleDBSession session;

    public SimpleDBAssociationIndexer(SimpleDBNativeItem nativeEntry, Association association, SimpleDBSession session) {
        this.nativeEntry = nativeEntry;
        this.association = association;
        this.session = session;
    }

    @Override
    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }

    @Override
    public void index(Object primaryKey, List foreignKeys) {
//        System.out.println("INDEX: index for id: "+primaryKey+", keys: "+foreignKeys+". entry: "+nativeEntry+", association: "+association);
    }

    @Override
    public List query(Object primaryKey) {
//        System.out.println("INDEX: query for id: "+primaryKey+". entry: "+nativeEntry+", association: "+association);
        return Collections.EMPTY_LIST;
    }

    @Override
    public void index(Object primaryKey, Object foreignKey) {
//        System.out.println("INDEX: index for id: "+primaryKey+", KEY: "+foreignKey+". entry: "+nativeEntry+", association: "+association);
    }
}
