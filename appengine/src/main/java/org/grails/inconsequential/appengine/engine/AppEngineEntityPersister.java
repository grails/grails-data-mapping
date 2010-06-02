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
package org.grails.inconsequential.appengine.engine;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import org.grails.inconsequential.appengine.AppEngineKey;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.engine.EntityAccess;
import org.grails.inconsequential.kv.engine.AbstractKeyValueEntityPesister;
import org.grails.inconsequential.mapping.ClassMapping;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link org.grails.inconsequential.engine.EntityPersister} abstract
 * class for AppEngine  
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AppEngineEntityPersister extends AbstractKeyValueEntityPesister<Entity, com.google.appengine.api.datastore.Key> {
    protected DatastoreService datastoreService;

    public AppEngineEntityPersister(PersistentEntity entity, DatastoreService datastoreService) {
        super(entity);
        this.datastoreService = datastoreService;
    }

    @Override
    protected Key createDatastoreKey(com.google.appengine.api.datastore.Key key) {
        return new AppEngineKey(key);
    }

    @Override
    protected Object getEntryValue(Entity nativeEntry, String propKey) {
        return nativeEntry.getProperty(propKey);
    }

    @Override
    protected void setEntryValue(Entity nativeEntry, String propKey, Object propValue) {
        nativeEntry.setProperty(propKey, propValue);
    }

    @Override
    protected Entity retrieveEntry(String family, Key key) {
        com.google.appengine.api.datastore.Key nativeKey = inferNativeKey(key.getNativeKey(), family);
        try {
            return datastoreService.get(nativeKey);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    @Override
    protected Entity createNewEntry(String family) {
        return new Entity(family);
    }

    @Override
    protected com.google.appengine.api.datastore.Key storeEntry(Entity nativeEntry) {
        return this.datastoreService.put(nativeEntry);
    }

    private com.google.appengine.api.datastore.Key inferNativeKey(Object nativeKey, String table) {
        if(nativeKey instanceof Long) {
            nativeKey = KeyFactory.createKey(table,(Long)nativeKey);
        }
        else if(!(nativeKey instanceof com.google.appengine.api.datastore.Key)) {
            nativeKey = KeyFactory.createKey(table,nativeKey.toString());
        }
        return (com.google.appengine.api.datastore.Key) nativeKey;
    }


    @Override
    protected void deleteEntities(MappingContext context, PersistentEntity persistentEntity, Object... objects) {
        if(objects != null) {
            List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
            final ClassMapping cm = persistentEntity.getMapping();
            final String family = getFamily(persistentEntity, cm);
            for (Object object : objects) {
               EntityAccess access = new EntityAccess(object);
               String idName = getIdentifierName(cm);
               final Object idValue = access.getProperty(idName);
               if(idValue != null) {
                   com.google.appengine.api.datastore.Key key = inferNativeKey(idValue, family);
                   keys.add(key);
               }
            }
            if(!keys.isEmpty()) {
                this.datastoreService.delete(keys.toArray(new com.google.appengine.api.datastore.Key[keys.size()]));
            }
        }
    }
}
