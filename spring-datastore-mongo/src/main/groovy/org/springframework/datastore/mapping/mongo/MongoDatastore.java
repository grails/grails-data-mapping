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

package org.springframework.datastore.mapping.mongo;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.datastore.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.document.mongodb.MongoFactoryBean;
import org.springframework.datastore.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValueMappingContext;
import org.springframework.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;

import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;

/**
 * A Datastore implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener{

	private Mongo mongo;
	private Map<PersistentEntity, MongoTemplate> mongoTemplates = new ConcurrentHashMap<PersistentEntity, MongoTemplate>();

	public MongoDatastore() {
		// TODO: Use DocumentMappingContext
		this(new KeyValueMappingContext("test"), Collections.<String, String>emptyMap());
	}
	
	

	public Mongo getMongo() {
		return mongo;
	}

	public MongoDatastore(MappingContext mappingContext,
			Map<String, String> connectionDetails) {
		super(mappingContext, connectionDetails);
		
		if(mappingContext != null)
			mappingContext.addMappingContextListener(this);

        initializeConverters(mappingContext);
	}

	public MongoDatastore(MappingContext mappingContext) {
		this(mappingContext, Collections.<String, String>emptyMap());
	}

	public MongoTemplate getMongoTemplate(PersistentEntity entity) {
		return mongoTemplates.get(entity);
	}
	@Override
	protected Session createSession(Map<String, String> connectionDetails) {
		return new MongoSession(this, getMappingContext());
	}

	public void afterPropertiesSet() throws Exception {
		MongoFactoryBean dbFactory = new MongoFactoryBean();
		dbFactory.afterPropertiesSet();
		
		this.mongo = dbFactory.getObject();
		
		for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
			createMongoTemplate(entity, mongo);
		}
	}

	protected void createMongoTemplate(PersistentEntity entity, Mongo mongoInstance) {
		KeyValueMappingContext kvmc = (KeyValueMappingContext) getMappingContext();
		MongoTemplate mt = new MongoTemplate(mongoInstance, kvmc.getKeyspace(),entity.getDecapitalizedName());
		try {
			mt.afterPropertiesSet();
		} catch (Exception e) {
			throw new DatastoreConfigurationException("Failed to configure Mongo template for entity ["+entity+"]: " + e.getMessage(),e);
		}
		
		mongoTemplates.put(entity, mt);

        initializeIndices(entity, mt);
	}

    /**
     * Indexes any properties that are mapped with index:true
     * @param entity The entity
     * @param template The template
     */
    protected void initializeIndices(final PersistentEntity entity, final MongoTemplate template) {
        template.execute(new DocumentStoreConnectionCallback<DB, Object>() {
            public Object doInConnection(DB db) throws Exception {
                final DBCollection collection = db.getCollection(template.getDefaultCollectionName());
                DBObject dbo = new BasicDBObject();
                dbo.put(MongoEntityPersister.MONGO_ID_FIELD, 1);
                collection.ensureIndex(dbo, entity.getIdentity().toString(), true);

                for (PersistentProperty property : entity.getPersistentProperties()) {
                    final boolean indexed = isIndexed(property) && Comparable.class.isAssignableFrom(property.getType());

                    if(indexed) {
                        DBObject dbObject = new BasicDBObject();
                        dbObject.put(property.getName(),1);
                        collection.ensureIndex(dbObject);
                    }
                }

                return null;
            }
        });
    }

    public void persistentEntityAdded(PersistentEntity entity) {
		createMongoTemplate(entity, this.mongo);
	}

}
