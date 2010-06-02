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

import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.engine.EntityAccess;
import org.grails.inconsequential.engine.EntityPersister;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.mapping.*;
import org.grails.inconsequential.mapping.types.Simple;

import java.util.List;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractKeyValueEntityPesister<T,K> extends EntityPersister {
    public AbstractKeyValueEntityPesister(PersistentEntity entity) {
        super(entity);
    }


    protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
        String table = null;
        if(cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if(table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }

    @Override
    protected Object retrieveEntity(MappingContext context, PersistentEntity persistentEntity, Key key) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = getFamily(persistentEntity, cm);

        T nativeEntry = retrieveEntry(family, key);
        if(nativeEntry != null) {
            Object obj = persistentEntity.newInstance();
            EntityAccess ea = new EntityAccess(obj);

            final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
            for (PersistentProperty prop : props) {
                if(prop instanceof Simple) {
                    PropertyMapping<KeyValue> pm = prop.getMapping();
                    String propKey;
                    if(pm.getMappedForm()!=null) {
                        propKey = pm.getMappedForm().getKey();
                    }
                    else {
                        propKey = prop.getName();
                    }
                    ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey) );
                }
            }
            return obj;
        }

        return null;
    }

    @Override
    protected Key persistEntity(MappingContext context, PersistentEntity persistentEntity, EntityAccess entityAccess) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = getFamily(persistentEntity, cm);

        T e = createNewEntry(family);
        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
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
        }

        K k = storeEntry(e);
        String id = getIdentifierName(cm);
        entityAccess.setProperty(id, k);
        return createDatastoreKey(k);
    }



    protected abstract Key createDatastoreKey(K k);

    protected String getIdentifierName(ClassMapping cm) {
        return cm.getIdentifier().getIdentifierName()[0];
    }

    protected abstract void setEntryValue(T nativeEntry, String propKey, Object propValue);


    protected abstract T createNewEntry(String family);

    /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a ColumnFamily, a BigTable entity, a Map etc.
     * @param propKey The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String propKey);

    /**
     * Reads the native form of a Key/value datastore entry. This could be
     * a ColumnFamily, a BigTable Entity, a Map etc.
     *
     * @param family The family
     * @param key The key
     * @return The native form
     */
    protected abstract T retrieveEntry(String family, Key key);

    /**
     * Stores the native form of a Key/value datastore to the actual data store
     *
     * @param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.
     * @return The native key
     */
    protected abstract K storeEntry(T nativeEntry);
}
