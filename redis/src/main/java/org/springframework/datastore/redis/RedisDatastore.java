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
package org.springframework.datastore.redis;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.datastore.core.AbstractDatastore;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.EntityAccess;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.engine.RedisEntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Datastore implementation for the Redis key/value datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastore extends AbstractDatastore implements InitializingBean {

    private boolean backgroundIndex;

    public RedisDatastore() {
        super(new KeyValueMappingContext(""));
    }

    public RedisDatastore(MappingContext mappingContext) {
        super(mappingContext);
    }

    public RedisDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        super(mappingContext, connectionDetails);
    }

    /**
     * Sets whether the Redis datastore should create indices in the background instead of on startup
     *
     * @param backgroundIndex True to create indices in the background
     */
    public void setBackgroundIndex(boolean backgroundIndex) {
        this.backgroundIndex = backgroundIndex;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new RedisSession(this, connectionDetails, getMappingContext());
    }

    public void afterPropertiesSet() throws Exception {
        if(backgroundIndex) {
            new Thread(new IndexOperation()).start();
        }
        else {
            new IndexOperation().run();
        }
    }

    /**
     * Used to update indices on startup or when a new entity is added
     */
    class IndexOperation implements Runnable {

        public void run() {
            final Session session = RedisDatastore.this.connect();
            final List<PersistentEntity> entities = RedisDatastore.this.getMappingContext().getPersistentEntities();
            for (PersistentEntity entity : entities) {
                final List<PersistentProperty> props = entity.getPersistentProperties();
                List<PersistentProperty> indexed = new ArrayList<PersistentProperty>();
                for (PersistentProperty prop : props) {
                    KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
                    if(kv != null && kv.isIndex()) {
                        indexed.add(prop);
                    }
                }

                if(!indexed.isEmpty()) {
                    // page through entities indexing each one
                    final Class cls = entity.getJavaClass();
                    Query query = session.createQuery(cls);
                    query.projections().count();
                    Long total = (Long) query.singleResult();

                    if(total < 100) {
                        List persistedObjects = session.createQuery(cls).list();
                        for (Object persistedObject : persistedObjects) {
                            updatedPersistedObjectIndices(session, entity, persistedObject, indexed);
                        }
                    }
                    else {
                        query = session.createQuery(cls);
                        int offset = 0;
                        int max = 100;

                        // 300+100 < 350
                        while(offset < total) {
                            query.offset(offset);
                            query.max(max);
                            List persistedObjects = query.list();
                            for (Object persistedObject : persistedObjects) {
                                updatedPersistedObjectIndices(session, entity, persistedObject, indexed);
                            }
                            
                            offset += max;
                        }

                    }
                }
            }
        }

        private void updatedPersistedObjectIndices(Session session, PersistentEntity entity, Object persistedObject, List<PersistentProperty> indexed) {
            EntityAccess ea = new EntityAccess(entity, persistedObject);
            Object identifier = ea.getIdentifier();
            for (PersistentProperty persistentProperty : indexed) {
                Object value = ea.getProperty(persistentProperty.getName());
                if(value != null) {
                    RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(entity.getJavaClass());
                    PropertyValueIndexer indexer = persister.getPropertyIndexer(persistentProperty);
                    indexer.index(value, identifier);
                }

            }
        }
    }
}
