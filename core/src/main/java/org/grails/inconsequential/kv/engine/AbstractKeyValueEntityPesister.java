/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.inconsequential.kv.engine;

import org.grails.inconsequential.core.ObjectDatastoreConnection;
import org.grails.inconsequential.engine.EntityAccess;
import org.grails.inconsequential.engine.EntityPersister;
import org.grails.inconsequential.engine.Indexer;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.mapping.*;
import org.grails.inconsequential.mapping.types.*;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for key/value style stores
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractKeyValueEntityPesister<T,K> extends EntityPersister {
    protected SimpleTypeConverter typeConverter;
    protected ObjectDatastoreConnection connection;

    public AbstractKeyValueEntityPesister(PersistentEntity entity, ObjectDatastoreConnection connection) {
        super(entity);
        this.connection = connection;
        this.typeConverter = new SimpleTypeConverter();
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(new Converter<byte[], Long>() {
            public Long convert(byte[] source) {
                try {
                    String value = new String(source, "UTF-8");
                    return Long.valueOf(value);
                } catch (UnsupportedEncodingException e) {
                    return 0L;
                }
            }
        });
        conversionService.addConverter(new Converter<byte[], String>() {
            public String convert(byte[] source) {
                try {
                    return new String(source, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        });
        this.typeConverter.setConversionService(conversionService);

    }

    public void setConversionService(ConversionService conversionService) {
        typeConverter.setConversionService(conversionService);
    }


    protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
        String table = null;
        if(cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if(table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }

    protected String getKeyspace(ClassMapping<Family> cm, String defaultValue) {
        String keyspace = null;
        if(cm.getMappedForm() != null) {
            keyspace = cm.getMappedForm().getKeyspace();
        }
        if(keyspace == null) keyspace = defaultValue;
        return keyspace;
    }


    @Override
    protected final void deleteEntities(MappingContext context, PersistentEntity persistentEntity, Object... objects) {
        if(objects != null) {
            List<K> keys = new ArrayList<K>();
            final ClassMapping cm = persistentEntity.getMapping();
            final String family = getFamily(persistentEntity, cm);
            for (Object object : objects) {
               EntityAccess access = new EntityAccess(object);
               access.setConversionService(typeConverter.getConversionService());
               String idName = getIdentifierName(cm);
               final Object idValue = access.getProperty(idName);
               if(idValue != null) {
                   K key = inferNativeKey(family, idValue);
                   keys.add(key);
               }
            }
            if(!keys.isEmpty()) {
                deleteEntries(family, keys);
            }
        }
    }


    @Override
    protected final Object retrieveEntity(MappingContext context, PersistentEntity persistentEntity, Serializable nativeKey) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = getFamily(persistentEntity, cm);

        T nativeEntry = retrieveEntry(persistentEntity, family, nativeKey);
        if(nativeEntry != null) {
            Object obj = persistentEntity.newInstance();

            EntityAccess ea = new EntityAccess(obj);
            ea.setConversionService(typeConverter.getConversionService());
            String idName = getIdentifierName(persistentEntity.getMapping());
            ea.setProperty(idName, nativeKey);

            final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
            for (PersistentProperty prop : props) {
                PropertyMapping<KeyValue> pm = prop.getMapping();
                String propKey;
                if(pm.getMappedForm()!=null) {
                    propKey = pm.getMappedForm().getKey();
                }
                else {
                    propKey = prop.getName();
                }
                if(prop instanceof Simple) {
                    ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey) );
                }
                else if(prop instanceof ToOne) {
                    Serializable associationKey = (Serializable) getEntryValue(nativeEntry, propKey);
                    Persister persister = connection.getPersister(prop.getType());
                    if(persister == null) {
                        throw new InvalidDataAccessApiUsageException("Cannot retrieve association ["+persistentEntity.getName()+"."+prop.getName()+"]. Class ["+prop.getType()+"] is not persistent type.");
                    }

                    ea.setProperty(prop.getName(), persister.retrieve(context, associationKey));
                }
                else if(prop instanceof OneToMany) {
                    Association association = (Association) prop;
                    if(association.getFetchStrategy() == Fetch.LAZY) {
                        // TODO: Handle lazy fetching
                    }
                    else {
                        Indexer indexer = getAssociationIndexer(association);
                        if(indexer != null) {
                            List keys = indexer.query(nativeKey);
                            Persister persister = connection.getPersister(association.getAssociatedEntity());
                            ea.setProperty( association.getName(), persister.retrieveAll(context, keys));
                        }
                    }

                }
            }
            return obj;
        }

        return null;
    }

    @Override
    protected final Serializable persistEntity(MappingContext context, PersistentEntity persistentEntity, EntityAccess entityAccess) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = getFamily(persistentEntity, cm);

        T e = createNewEntry(family);
        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        List<OneToMany> oneToManys = new ArrayList<OneToMany>();
        for (PersistentProperty prop : props) {
            if(prop instanceof Simple) {
                PropertyMapping<KeyValue> pm = prop.getMapping();
                String key = null;
                if(pm.getMappedForm() != null) {
                    key = pm.getMappedForm().getKey();
                }
                if(key == null) key = prop.getName();
                final Object propValue = entityAccess.getProperty(prop.getName());
                setEntryValue(e, key, propValue);
            }
            else if(prop instanceof OneToMany) {
                oneToManys.add((OneToMany) prop);
            }
            else if(prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if(association.doesCascade(Cascade.SAVE)) {

                    if(!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if(associatedObject != null) {

                            Persister persister = connection.getPersister(associatedObject);
                            if(persister == null) {
                                throw new InvalidDataAccessApiUsageException("Cannot persist association ["+persistentEntity.getName()+"."+prop.getName()+"]. Object ["+associatedObject+"] is not persistable.");
                            }
                            Serializable associationId = persister.persist(context, associatedObject);
                            setEntryValue(e, association.getName(), associationId);
                        }
                        else {
                            // TODO: throw exception if not nullable
                        }
                    }
                }

            }
        }

        K k = readObjectIdentifier(entityAccess, cm);
        if(k == null) {
            k = storeEntry(persistentEntity, e);
            String id = getIdentifierName(cm);
            entityAccess.setProperty(id, k);
        }
        else {
            updateEntry(persistentEntity, k, e);
        }

        for (OneToMany oneToMany : oneToManys) {
            if(oneToMany.doesCascade(Cascade.SAVE)) {
                Object propValue = entityAccess.getProperty(oneToMany.getName());

                if(propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;

                    Persister persister = connection.getPersister(oneToMany.getAssociatedEntity());
                    if(persister == null) {
                        throw new InvalidDataAccessApiUsageException("Cannot persist association ["+persistentEntity.getName()+"."+oneToMany.getName()+"]. Object ["+oneToMany.getAssociatedEntity().getName()+"] is not persistable.");
                    }

                    List<Serializable> keys = persister.persist(context, associatedObjects.toArray());

                    final Indexer indexer = getAssociationIndexer(oneToMany);
                    if(indexer != null) {
                        indexer.index(k, keys);
                    }
                }
            }

        }

        return (Serializable) k;
    }


    /**
     * Obtains an indexer for the given association
     *
     * @param association The association
     * @return An indexer
     */
    protected abstract Indexer getAssociationIndexer(Association association);


    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping<Family> cm) {
        String propertyName = getIdentifierName(cm);
        return (K) entityAccess.getProperty(propertyName);
    }


    protected String getIdentifierName(ClassMapping cm) {
        return cm.getIdentifier().getIdentifierName()[0];
    }

    /**
     * This is a rather simplistic and unoptimized implementation. Subclasses can override to provide
     * batch insert capabilities to optimize the insertion of multiple entities in one go
     * 
     * @param context The MappingContext
     * @param persistentEntity The persistent entity
     * @param objs The objext to persist
     * @return A list of keys
     */
    @Override
    protected List<Serializable> persistEntities(MappingContext context, PersistentEntity persistentEntity, Object... objs) {
        List<Serializable> keys = new ArrayList<Serializable>();
        for (Object obj : objs) {
            keys.add( persist(context,obj) );
        }
        return keys;
    }

    /**
     * Simplistic default implementation of retrieveAllEntities that iterates over each key and retrieves the entities 
     * one-by-one. Data stores that support batch retrieval can optimize this to retrieve all entities in one go.
     * 
     * @param context The context
     * @param persistentEntity The persist entity 
     * @param keys The keys
     * @return A list of entities
     */
    @Override
    protected List<Object> retrieveAllEntities(MappingContext context, PersistentEntity persistentEntity, List<Serializable> keys) {
        List<Object> results = new ArrayList<Object>();
        for (Serializable key : keys) {
            results.add( retrieveEntity(context, persistentEntity, key));
        }
        return results;
    }


    /**
     * Used to establish the native key to use from the identifier defined by the object
     * @param family The family
     * @param identifier The identifier specified by the object
     * @return The native key which may just be a cast from the identifier parameter to K
     */
    protected K inferNativeKey(String family, Object identifier) {
        return (K) identifier;
    }

    /**
     * Creates a new entry for the given family.
     *
     * @param family The family
     * @return An entry such as a BigTable Entity, ColumnFamily etc.
     */
    protected abstract T createNewEntry(String family);

    /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a ColumnFamily, a BigTable entity, a Map etc.
     * @param property The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String property);

    /**
     * Sets a value on an entry
     * @param nativeEntry The native entry such as a BigTable Entity, ColumnFamily etc.
     * @param key The key
     * @param value The value
     */
    protected abstract void setEntryValue(T nativeEntry, String key, Object value);

    /**
     * Reads the native form of a Key/value datastore entry. This could be
     * a ColumnFamily, a BigTable Entity, a Map etc.
     *
     * @param persistentEntity
     *@param family The family
     * @param key The key   @return The native form
     */
    protected abstract T retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key);

    /**
     * Stores the native form of a Key/value datastore to the actual data store
     *
     * @param persistentEntity
     *@param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.  @return The native key
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param key The key of the object to update
     * @param entry The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity, K key, T entry);

    /**
     * Deletes one or many entries for the given list of Keys
     *
     * @param family The family
     * @param keys The keys
     */
    protected abstract void deleteEntries(String family, List<K> keys);

}
