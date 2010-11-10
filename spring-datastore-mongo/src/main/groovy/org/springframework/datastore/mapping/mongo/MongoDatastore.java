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

import org.bson.types.ObjectId;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.data.document.mongodb.bean.factory.MongoFactoryBean;
import org.springframework.data.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.document.config.Collection;
import org.springframework.datastore.mapping.document.config.DocumentMappingContext;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;

import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.springframework.core.convert.converter.*;

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
		this(new DocumentMappingContext("test"), Collections.<String, String>emptyMap());
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
        mappingContext.getConverterRegistry().addConverter(new Converter<String, ObjectId>() {
			@Override
			public ObjectId convert(String source) {
				return new ObjectId(source);
			}
        });
        mappingContext.getConverterRegistry().addConverter(new Converter<ObjectId, String>() {
        	@Override
        	public String convert(ObjectId source) {
	        	return source.toString();
        	}
        });        
        
	}

	public MongoDatastore(MappingContext mappingContext) {
		this(mappingContext, Collections.<String, String>emptyMap());
	}
	
	/**
	 * Constructor for creating a MongoDatastore using an existing Mongo instance
	 * @param mappingContext
	 * @param mongo
	 */
	public MongoDatastore(MappingContext mappingContext, Mongo mongo) {
		this(mappingContext, Collections.<String, String>emptyMap());
		this.mongo = mongo;
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
		DocumentMappingContext dc = (DocumentMappingContext) getMappingContext();
		String collectionName = entity.getDecapitalizedName();
		ClassMapping<Collection> mapping = entity.getMapping();
		if(mapping.getMappedForm() != null && mapping.getMappedForm().getCollection() != null) {
			collectionName = mapping.getMappedForm().getCollection();
		}
		MongoTemplate mt = new MongoTemplate(mongoInstance, dc.getDefaultDatabaseName(),collectionName);
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
