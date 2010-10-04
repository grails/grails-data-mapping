package org.springframework.datastore.mapping.node.engine;


import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.core.SessionImplementor;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.EntityInterceptor;
import org.springframework.datastore.mapping.engine.LockableEntityPersister;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.model.types.OneToMany;
import org.springframework.datastore.mapping.model.types.Simple;
import org.springframework.datastore.mapping.model.types.ToOne;
import org.springframework.datastore.mapping.node.mapping.Node;
import org.springframework.datastore.mapping.node.mapping.NodeProperty;
import org.springframework.datastore.mapping.proxy.ProxyFactory;
import org.springframework.datastore.mapping.query.Query;

import javax.persistence.CascadeType;
import java.io.Serializable;
import java.util.*;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for Node style stores
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public abstract class AbstractNodeEntityPersister<T, K> extends LockableEntityPersister {
    protected Session session;
    protected ClassMapping classMapping;

    public AbstractNodeEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
        this.session = session;
        this.classMapping = entity.getMapping();
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return null;
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
    protected Serializable persistEntity(final PersistentEntity persistentEntity, Object obj) {
        ClassMapping<Node> cm = persistentEntity.getMapping();
        T tmp = null;
        final EntityAccess entityAccess = new EntityAccess(persistentEntity, obj);
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;

        if(!isUpdate) {
            System.out.println("!isUpdate");
            tmp = createNewEntry(persistentEntity);
            k = generateIdentifier(persistentEntity, tmp);
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);
        }
        else {
            SessionImplementor<T> si = (SessionImplementor<T>) session;
            tmp = si.getCachedEntry(persistentEntity, (Serializable) k);
            if(tmp == null) {
                //TODO If entity is update then update the entity
            }
            if(tmp == null) {
                tmp = createNewEntry(persistentEntity);
            }
        }

        final T e = tmp;

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        final Map<OneToMany, List<Serializable>> oneToManyKeys = new HashMap<OneToMany, List<Serializable>>();
        final Map<OneToMany, Serializable> inverseCollectionUpdates = new HashMap<OneToMany, Serializable>();
        final Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
        final Map<PersistentProperty, Object> toUnindex = new HashMap<PersistentProperty, Object>();
        for (PersistentProperty prop : props) {
            PropertyMapping<NodeProperty> pm = prop.getMapping();
            final NodeProperty nodeProperty = pm.getMappedForm();
            String key = null;
            String propName = null;
            if (nodeProperty != null) {
                propName = nodeProperty.getName();
            }
            final boolean indexed = nodeProperty != null && nodeProperty.isIndex();
            if(propName == null) propName = prop.getName();
            //Single Entity
            if(prop instanceof Simple) {
                Object propValue = entityAccess.getProperty(prop.getName());
                if(indexed) {
                    if(isUpdate) {
                        final Object oldValue = getEntryValue(e, key);
                        if(oldValue != null && !oldValue.equals(propValue))
                            toUnindex.put(prop, oldValue);
                    }

                    toIndex.put(prop, propValue);
                }
                setEntryValue(e, propName, propValue);
            }
            //TODO Implement OneToMany Relationship Entities
            else if(prop instanceof OneToMany) {
                final OneToMany oneToMany = (OneToMany) prop;

                Object propValue = entityAccess.getProperty(oneToMany.getName());

                if(propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;

                    List<Serializable> keys = session.persist(associatedObjects);

                    oneToManyKeys.put(oneToMany, keys);
                }
            }
            //TODO OneToOne Relationship Entities
            else if(prop instanceof ToOne) {                

            }// End
        }
      
        if(!isUpdate) {
            SessionImplementor si = (SessionImplementor) session;
            final K updateId = k;
            si.getPendingInserts().add(new Runnable() {
                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                            if(!interceptor.beforeInsert(persistentEntity, entityAccess)) return;
                    }
                      storeEntry(persistentEntity, updateId, e);                   
                }
            });
        }
        else {
            SessionImplementor si = (SessionImplementor) session;
            final K updateId = k;
            si.getPendingUpdates().add(new Runnable() {
                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                            if(!interceptor.beforeUpdate(persistentEntity, entityAccess)) return;
                    }
                    updateEntry(persistentEntity, updateId, e);
               }
            });
        }
        return (Serializable) k;
    }

    protected abstract K generateIdentifier(PersistentEntity persistentEntity, T tmp);


    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {

    }

    @Override
    protected void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {

    }

    @Override
    public Object lock(Serializable id) throws CannotAcquireLockException {
        return null;
    }

    @Override
    public Object lock(Serializable id, int timeout) throws CannotAcquireLockException {
        return null;
    }

    @Override
    public boolean isLocked(Object o) {
        return false;
    }

    @Override
    public void unlock(Object o) {

    }

    public Object proxy(Serializable key) {
        return null;
    }

    public Query createQuery() {
        return null;
    }

    public Serializable refresh(Object o) {
        return null;
    }

    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping<Node> cm) {
        return (K)entityAccess.getIdentifier();
    }

     /**
     * Obtains an indexer for the given association
     *
     * @param association The association
     * @return An indexer
     */
    public abstract AssociationIndexer getAssociationIndexer(Association association);


    protected String getIdentifierName(ClassMapping cm) {
        return cm.getIdentifier().getIdentifierName()[0];
    }

      /**
     * Used to establish the native key to use from the identifier defined by the object
     *
     * @param identifier The identifier specified by the object
     * @return The native key which may just be a cast from the identifier parameter to K
     */
    protected K inferNativeKey(Object identifier) {
        return (K) identifier;
    }


    /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a  JCR Node etc.
     * @param property    The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String property);

    /**
     * Reads the native form of a Node datastore entry. This could be
     * a JCR Node, a Graph Nodeetc.
     *
     * @param persistentEntity The persistent entity
     * @param key              The key
     * @return The native form
     */
    protected abstract T retrieveEntry(PersistentEntity persistentEntity, Serializable key);


    /**
     * Creates a new entry for the given Node.
     *
     * @param persistentEntity persistentEntity The persistent entity
     * @return An entry such as a JCR Node etc.
     */
    protected abstract T createNewEntry(PersistentEntity persistentEntity);

    /**
     * Sets a value on an entry
     *
     * @param nativeEntry  The native entry such as a JCR Node etc.
     * @param propertyName The Property Name
     * @param value        The value
     */
    protected abstract void setEntryValue(T nativeEntry, String propertyName, Object value);

    /**
     * Stores the native form of a Node style datastore to the actual data store
     *
     * @param persistentEntity The persistent entity
     * @param id               The id of the object to store
     * @param nativeEntry      The native form. Could be a a JCR Node,etc.
     * @return The native identitys
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, K id, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param id               The id of the object to update
     * @param entry            The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity, K id, T entry);
}
