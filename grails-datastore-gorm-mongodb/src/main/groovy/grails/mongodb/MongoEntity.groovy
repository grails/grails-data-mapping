/*
 * Copyright 2015 original authors
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
package grails.mongodb

import com.mongodb.AggregationOptions
import com.mongodb.MongoClient
import com.mongodb.ReadPreference
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.mongo.MongoCriteriaBuilder
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoCodecSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.engine.AbstractMongoObectEntityPersister
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.mongo.query.MongoQuery
/**
 * Enhances the default {@link GormEntity} class with MongoDB specific methods
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
trait MongoEntity<D> extends GormEntity<D> {


    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    def propertyMissing(String name) {
        getAt(name)
    }

    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    def propertyMissing(String name, val) {
        putAt(name, val)
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(String name, value) {
        if (hasProperty(name)) {
            ((GroovyObject)this).setProperty(name, value)
        }
        else {
            AbstractMongoSession session = (AbstractMongoSession)AbstractDatastore.retrieveSession(MongoDatastore)
            def persistentEntity = session.mappingContext.getPersistentEntity(getClass().name)
            SessionImplementor si = (SessionImplementor)session

            if (si.isStateless(persistentEntity)) {
                def coll = session.getCollection(persistentEntity)
                MongoEntityPersister persister = (MongoEntityPersister)session.getPersister(this)
                def id = persister.getObjectIdentifier(this)
                final updateObject = new Document('$set', new Document(name, value))
                coll.updateOne((Bson)new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD,id), updateObject)
            }
            else {
                final dbo = getDboInternal(this)
                dbo?.put name, value
                markDirty()
            }

        }
    }


    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    @CompileStatic
    def getAt(String name) {
        if (hasProperty(name)) {
            return ((GroovyObject)this).getProperty(name)
        }

        Document dbo = getDboInternal(this)
        if (dbo != null && dbo.containsKey(name)) {
            return dbo.get(name)
        }
        return null
    }


    @CompileStatic
    private Document getDboInternal(D instance) {
        AbstractMongoSession session = (AbstractMongoSession)AbstractDatastore.retrieveSession(MongoDatastore)

        if(session instanceof MongoCodecSession) {
            Document schemaless = (Document)session.getAttribute(instance, PersistentEntityCodec.SCHEMALESS_ATTRIBUTES)
            if(schemaless == null) {
                schemaless = new Document()
                session.setAttribute(instance, PersistentEntityCodec.SCHEMALESS_ATTRIBUTES, schemaless)
            }
            return schemaless
        }
        else {
            return getDbo()
        }
    }
    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The DBObject instance
     */
    Document getDbo() {
        AbstractMongoSession session = (AbstractMongoSession)AbstractDatastore.retrieveSession(MongoDatastore)
        // check first for embedded cached entries
        SessionImplementor<Document> si = (SessionImplementor<Document>) session;
        def persistentEntity = session.mappingContext.getPersistentEntity(getClass().name)
        Document dbo = (Document)si.getCachedEntry(persistentEntity, MongoEntityPersister.createEmbeddedCacheEntryKey(this))
        if(dbo != null) return dbo
        // otherwise check if instance is contained within session
        if (!session.contains(this)) {
            dbo = new Document()
            si.cacheEntry(persistentEntity, MongoEntityPersister.createInstanceCacheEntryKey(this), dbo)
            return dbo
        }

        EntityPersister persister = (EntityPersister)session.getPersister(this)
        def id = persister.getObjectIdentifier(this)
        dbo = (Document)((SessionImplementor)session).getCachedEntry(persister.getPersistentEntity(), id)
        if (dbo == null) {
            MongoCollection<Document> coll = session.getCollection(persistentEntity)
            dbo = coll.find((Bson)new Document(MongoEntityPersister.MONGO_ID_FIELD, id))
                    .limit(1)
                    .first()

        }
        return dbo
    }

    /**
     * @return Custom MongoDB criteria builder
     */
    static MongoCriteriaBuilder createCriteria() {
        (MongoCriteriaBuilder)withSession { Session session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            return new MongoCriteriaBuilder(entity.javaClass, session)
        }
    }

    /**
     * @return The database for this domain class
     */
    static MongoDatabase getDB() {
        (MongoDatabase)withSession({ AbstractMongoSession session ->
            def databaseName = session.getDatabase(session.mappingContext.getPersistentEntity(this.name))
            session.getNativeInterface()
                    .getDatabase(databaseName)

        })
    }

    /**
     * @return The name of the Mongo collection that entity maps to
     */
    static String getCollectionName() {
        (String)withSession({ AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            return session.getCollectionName(entity)
        })
    }

    /**
     * The actual collection that this entity maps to.
     *
     * @return The actual collection
     */
    static MongoCollection<Document> getCollection() {
        (MongoCollection<Document>)withSession { AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            return session.getCollection(entity)
        }
    }

    /**
     * Use the given collection for this entity for the scope of the closure call
     * @param collectionName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    static withCollection(String collectionName, Closure callable) {
        withSession { AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            final previous = session.useCollection(entity, collectionName)
            try {
                def dbName = session.getDatabase(entity)
                MongoClient mongoClient = (MongoClient) session.getNativeInterface()
                MongoDatabase db = mongoClient.getDatabase(dbName)
                def coll = db.getCollection(collectionName)
                return callable.call(coll)
            } finally {
                session.useCollection(entity, previous)
            }
        }
    }

    /**
     * Use the given collection for this entity for the scope of the session
     *
     * @param collectionName The collection name
     * @return The previous collection name
     */
    static String useCollection(String collectionName) {
        withSession { AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            session.useCollection(entity, collectionName)
        }
    }

    /**
     * Use the given database for this entity for the scope of the closure call
     * @param databaseName The collection name
     * @param callable The callable
     * @return The result of the closure
     */
    static withDatabase(String databaseName, Closure callable) {
        withSession { AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            final previous = session.useDatabase(entity, databaseName)
            try {
                MongoDatabase db = session.getNativeInterface().getDatabase(databaseName)
                return callable.call(db)
            } finally {
                session.useDatabase(entity, previous)
            }
        }
    }

    /**
     * Use the given database for this entity for the scope of the session
     *
     * @param databaseName The collection name
     * @return The previous database name
     */
    static String useDatabase(String databaseName) {
        withSession { AbstractMongoSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            session.useDatabase(entity, databaseName)
        }
    }

    /**
     * Counts the number of hits
     * @param query The query
     * @return The hit count
     */
    static int countHits(String query) {
        search(query).size()
    }

    /**
     * Execute a MongoDB aggregation pipeline. Note that the pipeline should return documents that represent this domain class as each return document will be converted to a domain instance in the result set
     *
     * @param pipeline The pipeline
     * @param options The options (optional)
     * @return A mongodb result list
     */
    static List<D> aggregate(List pipeline, AggregationOptions options = AggregationOptions.builder().build()) {
        (List<D>)withSession( { AbstractMongoSession session ->
            def persistentEntity = session.mappingContext.getPersistentEntity(this.name)
            def mongoCollection = session.getCollection(persistentEntity)
            if(session instanceof MongoCodecSession) {
                MongoDatastore datastore = (MongoDatastore)session.getDatastore()
                mongoCollection = mongoCollection
                        .withDocumentClass(persistentEntity.javaClass)
                        .withCodecRegistry(datastore.getCodecRegistry())
            }

            List<Document> newPipeline = cleanPipeline(pipeline)
            def aggregateIterable = mongoCollection.aggregate(newPipeline)
            if(options.allowDiskUse) {
                aggregateIterable.allowDiskUse(options.allowDiskUse)
            }
            if(options.batchSize) {
                aggregateIterable.batchSize(options.batchSize)
            }

            new MongoQuery.MongoResultList(aggregateIterable.iterator(), 0, (EntityPersister)session.getPersister(persistentEntity) as EntityPersister)
        } )
    }



    /**
     * Execute a MongoDB aggregation pipeline. Note that the pipeline should return documents that represent this domain class as each return document will be converted to a domain instance in the result set
     *
     * @param pipeline The pipeline
     * @param options The options (optional)
     * @return A mongodb result list
     */
    static List<D> aggregate(List pipeline, AggregationOptions options, ReadPreference readPreference) {
        (List<D>)withSession( { AbstractMongoSession session ->
            def persistentEntity = session.mappingContext.getPersistentEntity(this.name)
            List<Document> newPipeline = cleanPipeline(pipeline)
            def mongoCollection = session.getCollection(persistentEntity)
                    .withReadPreference(readPreference)
            def aggregateIterable = mongoCollection.aggregate(newPipeline)
            aggregateIterable.allowDiskUse(options.allowDiskUse)
            aggregateIterable.batchSize(options.batchSize)

            new MongoQuery.MongoResultList(aggregateIterable.iterator(), 0, (EntityPersister)session.getPersister(persistentEntity))
        } )
    }

    /**
     * Search for entities using the given query
     *
     * @param query The query
     * @return The results
     */
    static List<D> search(String query, Map options = Collections.emptyMap()) {
        (List<D>)withSession( { AbstractMongoSession session ->
            def persistentEntity = session.mappingContext.getPersistentEntity(this.name)
            def coll = session.getCollection(persistentEntity)
            if(session instanceof MongoCodecSession) {
                MongoDatastore datastore = (MongoDatastore)session.datastore
                coll = coll
                        .withDocumentClass(persistentEntity.javaClass)
                        .withCodecRegistry(datastore.codecRegistry)
            }
            def searchArgs = ['$search': query]
            if(options.language) {
                searchArgs['$language'] = options.language.toString()
            }
            def cursor = coll.find((Bson)new Document((Map<String,Object>)['$text': searchArgs]))

            int offset = options.offset instanceof Number ? ((Number)options.offset).intValue() : 0
            int max = options.max instanceof Number ? ((Number)options.max).intValue() : -1
            if(offset > 0) cursor.skip(offset)
            if(max > -1) cursor.limit(max)
            new MongoQuery.MongoResultList(cursor.iterator(), offset, (EntityPersister)session.getPersister(persistentEntity))
        } )
    }

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @param limit The maximum number of results. Defaults to 5.
     * @return The results
     */
    static List<D> searchTop(String query, int limit = 5, Map options = Collections.emptyMap()) {
        (List<D>)withSession( { AbstractMongoSession session ->
            def persistentEntity = session.mappingContext.getPersistentEntity(this.name)

            MongoCollection coll = session.getCollection(persistentEntity)
            if(session instanceof MongoCodecSession) {
                MongoDatastore datastore = (MongoDatastore)session.datastore
                coll = coll
                        .withDocumentClass(persistentEntity.javaClass)
                        .withCodecRegistry(datastore.codecRegistry)
            }
            EntityPersister persister = (EntityPersister)session.getPersister(persistentEntity)

            def searchArgs = ['$search': query]
            if(options.language) {
                searchArgs['$language'] = options.language.toString()
            }

            def score = new Document((Map<String,Object>)[score: ['$meta': 'textScore']])
            def cursor = coll.find((Bson)new Document((Map<String,Object>)['$text': searchArgs]))
                             .projection((Bson)score)
                    .sort((Bson)score)
                    .limit(limit)

            new MongoQuery.MongoResultList(cursor.iterator(), 0, persister)
        } )
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static withSession(Closure callable) {
        GormEnhancer.findStaticApi(this).withSession callable
    }

    @CompileStatic
    private static List<Document> cleanPipeline(List pipeline) {
        List<Document> newPipeline = new ArrayList<Document>()
        for (o in pipeline) {
            if (o instanceof Document) {
                newPipeline << (Document)o
            } else if (o instanceof Map) {
                newPipeline << new Document((Map) o)
            }
        }
        newPipeline
    }
}