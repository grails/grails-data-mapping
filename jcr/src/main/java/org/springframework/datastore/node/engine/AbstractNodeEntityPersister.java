package org.springframework.datastore.node.engine;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.collection.PersistentList;
import org.springframework.datastore.collection.PersistentSet;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.*;
import org.springframework.datastore.keyvalue.convert.ByteArrayAwareTypeConverter;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.*;
import org.springframework.datastore.mapping.types.*;
import org.springframework.datastore.node.mapping.Node;
import org.springframework.datastore.node.mapping.NodeProperty;
import org.springframework.datastore.proxy.LazyLoadingTargetSource;
import org.springframework.datastore.query.Query;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for Node style stores
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public abstract class AbstractNodeEntityPersister<T, K> extends EntityPersister {
    protected SimpleTypeConverter typeConverter;         
    protected Session session;
    protected ClassMapping classMapping;

    public AbstractNodeEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity);
        this.session = session;
        this.classMapping = entity.getMapping();
        this.typeConverter = new ByteArrayAwareTypeConverter();         
    }

    public ClassMapping getClassMapping() {
        return classMapping;
    }

     @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return null;  //TODO.
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
    protected Object retrieveEntity(PersistentEntity persistentEntity, Serializable nativeKey) {        
        T nativeEntry = retrieveEntry(persistentEntity, nativeKey);
        if(nativeEntry != null) {
            return createObjectFromNativeEntry(persistentEntity, nativeKey, nativeEntry);
        }
        return null;
    }

     private Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
        Object obj = persistentEntity.newInstance();

        EntityAccess ea = new EntityAccess(persistentEntity, obj);
        ea.setConversionService(typeConverter.getConversionService());
        String idName = ea.getIdentifierName();
        ea.setProperty(idName, nativeKey);

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (PersistentProperty prop : props) {
            PropertyMapping<NodeProperty> pm = prop.getMapping();
            String propName = null;
            if(pm.getMappedForm()!=null) {
                propName = pm.getMappedForm().getAttributeName();
            }
            if(propName == null) {
                propName = prop.getName();
            }
            if(prop instanceof Simple) {
                ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propName) );
            }
            //TODO: to handle these relational behavior OneToOne, OneToManny, ManyToMany sto map with NodeType entity such as JCR Node etc.
        }
        return obj;
    }

    @Override
    protected Serializable persistEntity(PersistentEntity persistentEntity, EntityAccess entityAccess) {
        ClassMapping<Node> cm = persistentEntity.getMapping();
        //passing simple name to to create new entity e.g TestEntity instead of org.springframework.datastore.jcr.test.TestEntity
        T e = createNewEntry(persistentEntity.getJavaClass().getSimpleName());
        
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;

        for (EntityInterceptor interceptor : interceptors) {
            if (isUpdate) {
                if (!interceptor.beforeUpdate(entityAccess.getEntity())) return (Serializable) k;
            } else {
                if (!interceptor.beforeInsert(entityAccess.getEntity())) return null;
            }
        }

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        List<OneToMany> oneToManys = new ArrayList<OneToMany>();

        Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
        for (PersistentProperty prop : props) {
            PropertyMapping<NodeProperty> pm = prop.getMapping();
            final NodeProperty nodeProperty = pm.getMappedForm();
            String propName = null;
            if (nodeProperty != null) {
                propName = nodeProperty.getAttributeName();
            }
            final boolean indexed = nodeProperty != null && nodeProperty.isIndex();
            if (propName == null)
                propName = prop.getName();
            if (prop instanceof Simple) {
                final Object propValue = entityAccess.getProperty(prop.getName());
                setEntryValue(e, propName, propValue);
                if (indexed) {
                    toIndex.put(prop, propValue);
                }
            //TODO: to handle these relational behavior to map with NodeType entity such as JCR Node etc.
            } else if (prop instanceof OneToMany) {
                oneToManys.add((OneToMany) prop);
            } else if (prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if (association.doesCascade(Cascade.SAVE)) {

                    if (!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if (associatedObject != null) {
                            Serializable associationId = session.persist(associatedObject);
                            //TODO: Implement to handle setEntryValue Method to support associationId
                            setEntryValue(e, propName, associationId);
                            if (indexed) {
                                toIndex.put(prop, associationId);
                            }
                        } else {
                            throw new DataIntegrityViolationException("Cannot save object [" + entityAccess.getEntity() + "] of type [" + persistentEntity + "]. The association [" + association + "] is cannot be null.");
                        }
                    }
                }

            }

        }


        if (k == null) {
            k = storeEntry(persistentEntity, e);
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);
        } else {
            updateEntry(persistentEntity, k, e);
        }
        return (Serializable) k;
    }

    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping cm) {
        return (K) entityAccess.getIdentifier();
    }



    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {
        //TODO Implement deleteEntity

    }

    @Override
    protected void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {
        //TODO Implement deleteEntities
    }

    public Query createQuery() {
        return null;
    }

       /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a  JCR Node etc.
     * @param property The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String property);

    /**
       * Reads the native form of a Node datastore entry. This could be
       * a JCR Node, a Graph Nodeetc.
       *
       * @param persistentEntity The persistent entity
       * @param key The key
       * @return The native form
       */
      protected abstract T retrieveEntry(PersistentEntity persistentEntity, Serializable key);


    /**
     * Creates a new entry for the given Node.
     *
     * @param name The Node name
     * @return An entry such as a JCR Node etc.
     */
    protected abstract T createNewEntry(String name);

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
     * @param nativeEntry      The native form. Could be a a JCR Node,etc.
     * @return The native identitys
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param id               The id of the object to update
     * @param entry            The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity, K id, T entry);
}
