package org.springframework.datastore.mapping.node.engine;


import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.mapping.collection.PersistentList;
import org.springframework.datastore.mapping.collection.PersistentSet;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.core.SessionImplementor;
import org.springframework.datastore.mapping.engine.*;
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValue;
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
import javax.persistence.FetchType;
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
    protected String nodeEntity;
    protected Session session;
    protected ClassMapping classMapping;


    public AbstractNodeEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
        this.session = session;
        this.classMapping = entity.getMapping();
        this.nodeEntity = getEntity(entity, classMapping);
    }

    public String getNodeEntity() {
        return nodeEntity;
    }

    public void setNodeEntity(String nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    protected String getEntity(PersistentEntity persistentEntity, ClassMapping<Node> cm) {
        String enName = null;
        if (cm.getMappedForm() != null) {
            enName = cm.getMappedForm().getEntityName();
        }
        if (enName == null) enName = persistentEntity.getJavaClass().getSimpleName();
        return enName;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return null;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys) {
        return null;
    }

    /**
     * This is a rather simplistic and unoptimized implementation. Subclasses can override to provide
     * batch insert capabilities to optimize the insertion of multiple entities in one go
     *
     * @param persistentEntity The persistent entity
     * @param objs             The objext to persist
     * @return A list of keys
     */
    @Override
    protected List<Serializable> persistEntities(PersistentEntity persistentEntity, Iterable objs) {
        List<Serializable> keys = new ArrayList<Serializable>();
        for (Object obj : objs) {
            keys.add(persist(obj));
        }
        return keys;
    }


    @Override
    protected Object retrieveEntity(PersistentEntity persistentEntity, Serializable key) {
        T nativeEntry = retrieveEntry(persistentEntity, key);
        if (nativeEntry != null) {
            return createObjectFromNativeEntry(persistentEntity, key, nativeEntry);
        }
        return null;
    }

    protected Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
        Object obj = persistentEntity.newInstance();
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry);
        return obj;
    }

    protected void refreshObjectStateFromNativeEntry(PersistentEntity persistentEntity, Object obj, Serializable nativeKey, T nativeEntry) {
        EntityAccess ea = new EntityAccess(persistentEntity, obj);
        ea.setConversionService(getMappingContext().getConversionService());
        String idName = ea.getIdentifierName();
        ea.setProperty(idName, nativeKey);

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (final PersistentProperty prop : props) {
            PropertyMapping<NodeProperty> pm = prop.getMapping();
            String propKey = null;
            if (pm.getMappedForm() != null) {
                propKey = pm.getMappedForm().getName();
            }
            if (propKey == null) {
                propKey = prop.getName();
            }
            if (prop instanceof Simple) {
                ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey));
            } else if (prop instanceof ToOne) {
                Serializable tmp = (Serializable) getEntryValue(nativeEntry, propKey);
                PersistentEntity associatedEntity = prop.getOwner();
                final Serializable associationKey = (Serializable) getMappingContext().getConversionService().convert(tmp, associatedEntity.getIdentity().getType());
                if (associationKey != null) {
                    PropertyMapping<NodeProperty> associationPropertyMapping = prop.getMapping();
                    boolean isLazy = isLazyAssociation(associationPropertyMapping);
                    final Class propType = prop.getType();
                    if (isLazy) {
                        Object proxy = getProxyFactory().createProxy(session, propType, associationKey);
                        ea.setProperty(prop.getName(), proxy);
                    } else {
                        ea.setProperty(prop.getName(), session.retrieve(propType, associationKey));
                    }
                }
            } else if (prop instanceof OneToMany) {
                Association association = (Association) prop;
                PropertyMapping<NodeProperty> associationPropertyMapping = association.getMapping();

                boolean isLazy = isLazyAssociation(associationPropertyMapping);
                //AssociationIndexer indexer = getAssociationIndexer(association);
                nativeKey = (Serializable) getMappingContext().getConversionService().convert(nativeKey, getPersistentEntity().getIdentity().getType());
                if (isLazy) {
                    if (List.class.isAssignableFrom(association.getType())) {
                        // ea.setPropertyNoConversion(association.getName(), new PersistentList(nativeKey, session));
                    } else if (Set.class.isAssignableFrom(association.getType())) {
                        // ea.setPropertyNoConversion(association.getName(), new PersistentSet(nativeKey, session, indexer));
                    }
                } else {
                    //if(indexer != null) {
                    //    List keys = indexer.query(nativeKey);
                    ea.setProperty(association.getName(), session.retrieveAll(association.getAssociatedEntity().getJavaClass(), nativeKey));
                    // }
                }

            }
        }
    }


    private boolean isLazyAssociation(PropertyMapping<NodeProperty> associationPropertyMapping) {
        if (associationPropertyMapping != null) {
            NodeProperty np = associationPropertyMapping.getMappedForm();
            if (np.getFetchStrategy() != FetchType.LAZY) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected Serializable persistEntity(final PersistentEntity persistentEntity, Object obj) {
        ClassMapping<Node> cm = persistentEntity.getMapping();
        T tmp = null;
        final EntityAccess entityAccess = new EntityAccess(persistentEntity, obj);
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;
        if (!isUpdate) {
            tmp = createNewEntry(persistentEntity);
            k = generateIdentifier(persistentEntity, tmp);
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);
        } else {
            SessionImplementor<T> si = (SessionImplementor<T>) session;
            tmp = si.getCachedEntry(persistentEntity, (Serializable) k);
            if (tmp == null) {
                tmp = retrieveEntry(persistentEntity, (Serializable) k);
            }
            if (tmp == null) {
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
            String propName = null;
            if (nodeProperty != null) {
                propName = nodeProperty.getName();
            }
            final boolean indexed = nodeProperty != null && nodeProperty.isIndex();
            if (propName == null) propName = prop.getName();
            //Single Entity
            if (prop instanceof Simple) {
                Object propValue = entityAccess.getProperty(prop.getName());
                if (indexed) {
                    if (isUpdate) {
                        final Object oldValue = getEntryValue(e, propName);
                        if (oldValue != null && !oldValue.equals(propValue))
                            toUnindex.put(prop, oldValue);
                    }

                    toIndex.put(prop, propValue);
                }
                setEntryValue(e, propName, propValue);
            } else if (prop instanceof OneToMany) {
                final OneToMany oneToMany = (OneToMany) prop;

                Object propValue = entityAccess.getProperty(oneToMany.getName());

                if (propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;

                    List<Serializable> keys = session.persist(associatedObjects);

                    oneToManyKeys.put(oneToMany, keys);
                }
            } else if (prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if (association.doesCascade(CascadeType.PERSIST)) {

                    if (!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if (associatedObject != null) {
                            ProxyFactory proxyFactory = getProxyFactory();
                            // never cascade to proxies
                            if (!proxyFactory.isProxy(associatedObject)) {
                                Serializable associationId;
                                AbstractNodeEntityPersister associationPersister = (AbstractNodeEntityPersister) session.getPersister(associatedObject);
                                if (!session.contains(associatedObject)) {
                                    Serializable tempId = associationPersister.getObjectIdentifier(associatedObject);
                                    if (tempId == null) tempId = session.persist(associatedObject);
                                    associationId = tempId;
                                } else {
                                    associationId = associationPersister.getObjectIdentifier(associatedObject);                                 }

                                if (indexed) {
                                    toIndex.put(prop, associationId);
                                    if (isUpdate) {
                                        final Object oldValue = getEntryValue(e, propName);
                                        if (oldValue != null && !oldValue.equals(associatedObject))
                                            toUnindex.put(prop, oldValue);
                                    }
                                }
                                //setEntryAssociatedValue(e, propName, associationId);
                                 setEntryValue(e, propName,associationId);
                                if (association.isBidirectional()) {
                                    Association inverse = association.getInverseSide();
                                    if (inverse instanceof OneToMany) {
                                        inverseCollectionUpdates.put((OneToMany) inverse, associationId);
                                    }
                                }

                            }
                        } else {
                            if (!association.isNullable() && !association.isCircular()) {
                                throw new DataIntegrityViolationException("Cannot save object [" + entityAccess.getEntity() + "] of type [" + persistentEntity + "]. The association [" + association + "] is cannot be null.");
                            }
                        }
                    }
                }
            }// End
        }

        if (!isUpdate) {
            SessionImplementor si = (SessionImplementor) session;
            final K updateId = k;
            si.getPendingInserts().add(new Runnable() {
                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                        if (!interceptor.beforeInsert(persistentEntity, entityAccess)) return;
                    }
                    storeEntry(persistentEntity, updateId, e);
                }
            });
        } else {
            SessionImplementor si = (SessionImplementor) session;
            final K updateId = k;
            si.getPendingUpdates().add(new Runnable() {
                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                        if (!interceptor.beforeUpdate(persistentEntity, entityAccess)) return;
                    }
                    updateEntry(persistentEntity, updateId, e);
                }
            });
        }
        return (Serializable) k;
    }




    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {
        if (obj != null) {
            for (EntityInterceptor interceptor : interceptors) {
                if (!interceptor.beforeDelete(persistentEntity, createEntityAccess(persistentEntity, obj))) return;
            }

            final K key = readIdentifierFromObject(obj);

            if (key != null) {
                deleteEntry(key);
            }

        }
    }

    private K readIdentifierFromObject(Object object) {
        EntityAccess access = new EntityAccess(getPersistentEntity(), object);
        access.setConversionService(getMappingContext().getConversionService());
        final Object idValue = access.getIdentifier();
        K key = null;
        if (idValue != null) {
            key = inferNativeKey(idValue);
        }
        return key;
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


    public Serializable refresh(Object o) {
        return null;
    }

    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping<Node> cm) {
        return (K) entityAccess.getIdentifier();
    }

    /**
     * Sets an Association entry on an entry
     *
     * @param nativeEntry  The native entry such as a JCR Node etc.
     * @param propertyName The Property Name
     * @param associationId  The Association Id

    protected abstract void setEntryAssociatedValue(T nativeEntry, String propertyName, Serializable associationId);
    */

    protected abstract K generateIdentifier(PersistentEntity persistentEntity, T tmp);

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
     * Deletes a single entry
     *
     * @param key The identity
     */
    protected abstract void deleteEntry(K key);


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
