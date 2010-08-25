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
package org.springframework.datastore.appengine.engine;

import com.google.appengine.api.datastore.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.datastore.appengine.AppEngineSession;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Association;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link org.springframework.datastore.engine.EntityPersister} abstract
 * class for AppEngine  
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AppEngineEntityPersister extends AbstractKeyValueEntityPesister<Entity, com.google.appengine.api.datastore.Key> {
    protected DatastoreService datastoreService;
    protected String entityFamily;

    public AppEngineEntityPersister(MappingContext context, final PersistentEntity entity, AppEngineSession conn, DatastoreService datastoreService) {
        super(context, entity, conn);
        this.datastoreService = datastoreService;
        this.entityFamily = getFamily(entity, entity.getMapping());
        GenericConversionService conversionService = (GenericConversionService) typeConverter.getConversionService();

        conversionService.addConverter(new Converter<Object, com.google.appengine.api.datastore.Key>() {
            public com.google.appengine.api.datastore.Key convert(Object source) {
                if(source instanceof com.google.appengine.api.datastore.Key) {
                     return (com.google.appengine.api.datastore.Key)source;
                }
                else if(source instanceof Long) {
                    return KeyFactory.createKey(entityFamily, (Long) source);
                }
                else {
                    return KeyFactory.createKey(entityFamily,source.toString());
                }
            }
        });
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
    protected Entity retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativekey) {
        com.google.appengine.api.datastore.Key nativeKey = inferNativeKey(family, nativekey);
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
    protected com.google.appengine.api.datastore.Key storeEntry(PersistentEntity persistentEntity, Key storeId, Entity nativeEntry) {
        return this.datastoreService.put(nativeEntry);
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, com.google.appengine.api.datastore.Key key, Entity entry) {
        if(entry != null) {
            Entity existing = getEntity(key);
            final Map<String,Object> props = entry.getProperties();
            for (String name : props.keySet()) {
                existing.setProperty(name, props.get(name));
            }
            datastoreService.put(existing);
        }
    }

    @Override
    protected void deleteEntry(String family, Key key) {
        datastoreService.delete(key);
    }

    @Override
    public AssociationIndexer getAssociationIndexer(Association association) {
        return null;  // TODO: Support one-to-many associations in GAE
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // TODO: GAE natively supports creating indices so not sure implementing this will be useful
        return null;
    }

    @Override
    protected Key generateIdentifier(PersistentEntity persistentEntity, Entity entity) {
        return datastoreService.put(entity);
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

    public org.springframework.datastore.query.Query createQuery() {
        return null;  // TODO: Implement querying for GAE
    }
}
