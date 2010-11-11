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

import static org.springframework.datastore.mapping.config.utils.ConfigUtils.read;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.document.DocumentStoreConnectionCallback;
import org.springframework.data.document.mongodb.DBCallback;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.data.document.mongodb.bean.factory.MongoFactoryBean;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.document.config.DocumentMappingContext;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.mongo.config.MongoCollection;
import org.springframework.datastore.mapping.mongo.config.MongoMappingContext;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

/**
 * A Datastore implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener{

	public static final String PASSWORD = "password";
	public static final String USERNAME = "username";
	public static final String MONGO_PORT = "port";
	public static final String MONGO_HOST = "host";
	
	private Mongo mongo;
	private MongoOptions mongoOptions = new MongoOptions();
	private Map<PersistentEntity, MongoTemplate> mongoTemplates = new ConcurrentHashMap<PersistentEntity, MongoTemplate>();

	/**
	 * Constructs a MongoDatastore using the default database name of "test" and defaults for the host and port.
	 * Typically used during testing. 
	 */
	public MongoDatastore() {
		this(new MongoMappingContext("test"), Collections.<String, String>emptyMap());
	}
	
	/**
	 * Constructs a MongoDatastore using the given MappingContext and connection details map.
	 * 
	 * @param mappingContext The MongoMappingContext
	 * @param connectionDetails The connection details containing the {@link #MONGO_HOST} and {@link #MONGO_PORT} settings 
	 */	
	public MongoDatastore(MongoMappingContext mappingContext,
			Map<String, String> connectionDetails, MongoOptions mongoOptions) {

		this(mappingContext, connectionDetails);
		if(mongoOptions != null)
			this.mongoOptions = mongoOptions;
	}
	
	/**
	 * Constructs a MongoDatastore using the given MappingContext and connection details map.
	 * 
	 * @param mappingContext The MongoMappingContext
	 * @param connectionDetails The connection details containing the {@link #MONGO_HOST} and {@link #MONGO_PORT} settings 
	 */
	public MongoDatastore(MongoMappingContext mappingContext,
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

	public MongoDatastore(MongoMappingContext mappingContext) {
		this(mappingContext, Collections.<String, String>emptyMap());
	}
	
	/**
	 * Constructor for creating a MongoDatastore using an existing Mongo instance
	 * @param mappingContext The MappingContext
	 * @param mongo The existing Mongo instance
	 */
	public MongoDatastore(MongoMappingContext mappingContext, Mongo mongo) {
		this(mappingContext, Collections.<String, String>emptyMap());
		this.mongo = mongo;
	}	
	
	/**
	 * Constructor for creating a MongoDatastore using an existing Mongo instance. In this case
	 * the connection details are only used to supply a USERNAME and PASSWORD
	 * 
	 * @param mappingContext The MappingContext
	 * @param mongo The existing Mongo instance
	 */
	public MongoDatastore(MongoMappingContext mappingContext, Mongo mongo, Map<String, String> connectionDetails) {
		this(mappingContext, connectionDetails);
		this.mongo = mongo;
	}		

	
	public Mongo getMongo() {
		return mongo;
	}

	public MongoTemplate getMongoTemplate(PersistentEntity entity) {
		return mongoTemplates.get(entity);
	}
	
	@Override
	protected Session createSession(Map<String, String> connectionDetails) {
		return new MongoSession(this, getMappingContext());
	}

	public void afterPropertiesSet() throws Exception {
		if(this.mongo == null) {
			ServerAddress defaults = new ServerAddress();
			MongoFactoryBean dbFactory = new MongoFactoryBean();
			dbFactory.setHost( read(String.class, MONGO_HOST, connectionDetails, defaults.getHost()) );
			dbFactory.setPort( read(Integer.class, MONGO_PORT, connectionDetails, defaults.getPort()) );
			if(mongoOptions != null ) {
				dbFactory.setMongoOptions(mongoOptions);
			}
			dbFactory.afterPropertiesSet();
			
			this.mongo = dbFactory.getObject();
		}
		
		
		
		for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
			createMongoTemplate(entity, mongo);
		}
	}

	protected void createMongoTemplate(PersistentEntity entity, Mongo mongoInstance) {
		DocumentMappingContext dc = (DocumentMappingContext) getMappingContext();
		String collectionName = entity.getDecapitalizedName();
		String databaseName = dc.getDefaultDatabaseName();
		ClassMapping<MongoCollection> mapping = entity.getMapping();
		final MongoCollection mongoCollection = mapping.getMappedForm() != null ? mapping.getMappedForm() : null;
		
		if(mongoCollection != null) {
			if(mongoCollection.getCollection() != null)
				collectionName = mongoCollection.getCollection();
			if(mongoCollection.getDatabase() != null)
				databaseName = mongoCollection.getDatabase();
			
		}
		final MongoTemplate mt = new MongoTemplate(mongoInstance, databaseName,collectionName);
		
		String username = read(String.class, USERNAME, connectionDetails, null);
		String password = read(String.class, PASSWORD, connectionDetails, null);
		
		if(username != null && password != null) {
			mt.setUsername(username);
			mt.setPassword(password.toCharArray());
		}
		
		if(mongoCollection != null) {	
			final WriteConcern writeConcern = mongoCollection.getWriteConcern();
			final String shardPropertyName = mongoCollection.getShard();
			if(writeConcern != null || shardPropertyName != null) {
				mt.executeInSession(new DBCallback<Object>() {
					@Override
					public Object doInDB(DB db) throws MongoException,
							DataAccessException {
						
						if(writeConcern != null) {
							DBCollection collection = db.getCollection(mt.getDefaultCollectionName());
							collection.setWriteConcern(writeConcern);							
						}						
						if(shardPropertyName != null) {
						
							db.command(new BasicDBObject("enablesharding", db.getName()));
							final BasicDBObject shardCollectionCommand = new BasicDBObject("shardcollection", mt.getDefaultCollectionName());
							shardCollectionCommand.put("key", shardPropertyName);
							
							db.command(shardCollectionCommand);
						}
						return null;
					}
				});				
			}
			
		}

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
