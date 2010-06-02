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
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import org.grails.inconsequential.appengine.AppEngineKey;
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
public class AppEngineEntityPersister extends EntityPersister {
    protected DatastoreService datastoreService;

    public AppEngineEntityPersister(PersistentEntity entity, DatastoreService datastoreService) {
        super(entity);
        this.datastoreService = datastoreService;
    }

    @Override
    protected Object retrieveEntity(MappingContext context, PersistentEntity persistentEntity, Key key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Key persistEntity(MappingContext context, PersistentEntity persistentEntity, EntityAccess entityAccess) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String table = null;
        if(cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if(table == null) table = persistentEntity.getJavaClass().getName();

        Entity e = new Entity(table);
        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (PersistentProperty prop : props) {
            if(prop instanceof Simple) {
                PropertyMapping<KeyValue> pm = prop.getMapping();
                String key = null;
                if(pm != null && pm.getMappedForm() != null) {
                    key = pm.getMappedForm().getKey();
                }
                if(key == null) key = prop.getName();
                e.setProperty(key, entityAccess.getProperty(prop.getName()));                
            }
        }

        com.google.appengine.api.datastore.Key k = datastoreService.put(e);
        String id = cm.getIdentifier().getIdentifierName()[0];
        entityAccess.setProperty(id, k);
        return new AppEngineKey(k);
    }

    @Override
    protected void deleteEntities(MappingContext context, PersistentEntity persistentEntity, Object... objects) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
