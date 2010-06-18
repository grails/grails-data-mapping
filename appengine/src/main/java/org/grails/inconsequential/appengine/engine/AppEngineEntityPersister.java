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
import org.grails.inconsequential.kv.engine.AbstractKeyValueEntityPesister;
import org.grails.inconsequential.mapping.PersistentEntity;

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
    protected Object getEntryValue(Entity nativeEntry, String property) {
        return nativeEntry.getProperty(property);
    }

    @Override
    protected void setEntryValue(Entity nativeEntry, String propKey, Object value) {
        nativeEntry.setProperty(propKey, value);
    }

    @Override
    protected Entity retrieveEntry(PersistentEntity persistentEntity, String family, Key key) {
        com.google.appengine.api.datastore.Key nativeKey = inferNativeKey(family, key.getNativeKey());
        return getEntity(nativeKey);
    }

    private Entity getEntity(com.google.appengine.api.datastore.Key nativeKey) {
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
    protected com.google.appengine.api.datastore.Key storeEntry(PersistentEntity persistentEntity, Entity nativeEntry) {
        return this.datastoreService.put(nativeEntry);
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, com.google.appengine.api.datastore.Key key, Entity entry) {
        if(entry != null) {
            datastoreService.put(entry);
        }
    }

    @Override
    protected com.google.appengine.api.datastore.Key inferNativeKey(String family, Object identifier) {
        if(identifier instanceof Long) {
            identifier = KeyFactory.createKey(family,(Long) identifier);
        }
        else if(!(identifier instanceof com.google.appengine.api.datastore.Key)) {
            identifier = KeyFactory.createKey(family, identifier.toString());
        }
        return (com.google.appengine.api.datastore.Key) identifier;
    }

    @Override
    protected void deleteEntries(String family, List<com.google.appengine.api.datastore.Key> keys) {
        this.datastoreService.delete(keys.toArray(new com.google.appengine.api.datastore.Key[keys.size()]));
    }
}
