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
package org.grails.datastore.gorm.mongo.extensions
import com.mongodb.*
import com.mongodb.client.DistinctIterable
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.MongoIterable
import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.FindOneAndDeleteOptions
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.engine.AbstractMongoObectEntityPersister
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.query.MongoQuery

import java.util.concurrent.TimeUnit

import static java.util.Arrays.asList
import static java.util.concurrent.TimeUnit.MILLISECONDS
/**
 * Extra methods for MongoDB API
 *
 * This extension makes it possible to use Groovy's map syntax instead of having to construct {@link org.bson.BSONObject} instances
 *
 * @author Graeme Rocher
 * @since 4.0.5
 */
@CompileStatic
class MongoExtensions {


    static <T> T asType(Document document, Class<T> cls) {
        def datastore = GormEnhancer.findDatastore(cls)
        AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession
        if (session != null) {
            return session.decode(cls, document)
        }
        else if(cls.name == 'grails.converters.JSON') {
            return cls.newInstance( document )
        }
        else {
            throw new IllegalArgumentException("Cannot convert DBOject [$document] to target type $cls. Type is not a persistent entity")
        }
    }

    static <T> T asType(FindIterable iterable, Class<T> cls) {
        def datastore = GormEnhancer.findDatastore(cls)
        AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession

        if (session != null) {
            return session.decode(cls, iterable)
        }
        else {
            throw new IllegalArgumentException("Cannot convert DBOject [$iterable] to target type $cls. Type is not a persistent entity")
        }
    }

    static <T> List<T> toList(FindIterable iterable, Class<T> cls) {
        def datastore = GormEnhancer.findDatastore(cls)
        AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession

        MongoEntityPersister p = (MongoEntityPersister)session.getPersister(cls)
        if (p)
            return new MongoQuery.MongoResultList(((FindIterable<Document>)iterable).iterator(),0,p)
        else {
            throw new IllegalArgumentException("Cannot convert DBCursor [$iterable] to target type $cls. Type is not a persistent entity")
        }
    }

    @CompileStatic
    static DBObject toDBObject(Document document) {
        def object = new BasicDBObject()
        for(key in document.keySet()) {
            def value = document.get(key)
            if(value instanceof Document) {
                value = toDBObject((Document)value)
            }
            else if(value instanceof Collection) {
                Collection col = (Collection)value
                Collection newCol = []
                for(i in col) {
                    if(i instanceof Document) {
                        newCol << toDBObject((Document)i)
                    }
                    else {
                        newCol << i
                    }
                }
                value = newCol
            }
            object.put(key, value)
        }
        return object
    }
    static DistinctIterable<Document> filter(DistinctIterable<Document> iterable, Map<String,Object> filter) {
        iterable.filter(new Document(filter))
    }

    static FindIterable<Document> filter(FindIterable<Document> iterable, Map<String,Object> filter) {
        iterable.filter(new Document(filter))
    }

    static FindIterable<Document> modifiers(FindIterable<Document> iterable, Map<String,Object> modifiers) {
        iterable.modifiers(new Document(modifiers))
    }

    static FindIterable<Document> projection(FindIterable<Document> iterable, Map<String,Object> projection) {
        iterable.projection(new Document(projection))
    }

    static  FindIterable<Document> sort(FindIterable<Document> iterable, Map<String,Object> sort) {
        iterable.sort(new Document(sort))
    }

    static DBCursor sort(DBCursor cursor, Map<String,Object> sort) {
        cursor.sort( (DBObject)new BasicDBObject(sort) )
    }

    static DBCursor hint(DBCursor cursor, Map<String,Object> sort) {
        cursor.hint( (DBObject)new BasicDBObject(sort) )
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link DBCollection}
     */
    static Object getProperty(DB db, String name) {
        db.getCollection(name)
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link DBCollection}
     */
    static Object getProperty(MongoDatabase db, String name) {
        db.getCollection(name)
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link DBCollection}
     */
    static Object getAt(DB db, String name) {
        db.getCollection(name)
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link DBCollection}
     */
    static Object getAt(MongoDatabase db, String name) {
        db.getCollection(name)
    }

    static MongoIterable<String> getCollectionNames(MongoDatabase db) {
        db.listCollectionNames()
    }

    static DBCollection createCollection(DB db, final String collectionName, final Map options) {
        db.createCollection(collectionName, (DBObject)new BasicDBObject(options))
    }

    static void createCollection(MongoDatabase db, final String collectionName, final Map<String, Object> options) {
        db.createCollection(collectionName, mapToObject(CreateCollectionOptions, options))
    }

    @CompileDynamic
    public static <T> T mapToObject(Class<T> targetType, Map<String,Object> values) {
        T t = targetType.newInstance()
        for(String name in values.keySet()) {
            if(t.respondsTo(name)) {
                t."$name"( values.get(name) )
            }
        }
        return t
    }


    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query) {
        collection.findOne((DBObject)new BasicDBObject(query))
    }



    static String getName(MongoCollection<Document> collection) {
        collection.namespace.collectionName
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static Document findOne(MongoCollection<Document> collection, final Map<String, Object> query) {
        collection.find( (Bson)new Document(query) ).limit(1).first()
    }

    static Document findOne(MongoCollection<Document> collection, ObjectId id) {
        def query = new Document()
        query.put(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)
        collection.find((Bson)query)
                  .limit(1)
                  .first()
    }

    static Document findOne(MongoCollection<Document> collection, CharSequence id) {
        def query = new Document()
        query.put(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)

        collection.find((Bson)query)
                .limit(1)
                .first()
    }

    static <T> T findOne(MongoCollection<Document> collection, Serializable id, Class<T> type) {
        def query = new Document()
        query.put(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)

        collection
                .find((Bson)query, type)
                .limit(1)
                .first()
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static Document findOne(MongoCollection<Document> collection, final Map<String,Object> query, final Map projection) {
        collection
                .find( (Bson)new Document(query) )
                .projection( new Document(projection) )
                .limit(1)
                .first()
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection, final Map sort) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), (DBObject)new BasicDBObject(sort))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort) {
        collection
                .find( (Bson)new Document(query) )
                .projection( new Document(projection) )
                .sort( new Document(sort) )
                .limit(1)
                .first()
    }

    static Document findOne(MongoCollection<Document> collection) {
        collection
                .find()
                .first()
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection, final ReadPreference readPreference) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection),readPreference)
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference)
     */
    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .find( (Bson)new Document(query) )
                .projection( new Document(projection) )
                .limit(1)
                .first()
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection, final Map sort,
                            final ReadPreference readPreference) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), (DBObject)new BasicDBObject(sort), readPreference)
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference)
     */
    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort,
                            final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .find( (Bson)new Document(query) )
                .projection( new Document(projection) )
                .sort( new Document(sort) )
                .limit(1)
                .first()
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static DBCursor find(DBCollection collection, final Map query) {
        collection.find((DBObject)new BasicDBObject(query))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static FindIterable<Document> find(MongoCollection<Document> collection, final Map<String, Object> query) {
        collection.find((Bson)new Document(query))
    }

    static <T>  FindIterable<T> find(MongoCollection<T> collection, final Map<String, Object> query, Class<T> type) {
        collection.find((Bson)new Document(query), type)
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBCursor find(DBCollection collection, final Map<String, Object> query, final Map<String, Object> projection) {
        collection.find((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static FindIterable<Document> find(MongoCollection<Document> collection, final Map<String, Object> query, final Map<String, Object> projection) {
        collection.find((Bson)new Document(query))
                  .projection( new Document(projection) )
    }


    static long count(DBCollection collection, final Map query) {
        getCount(collection, query, null)
    }

    static long count(MongoCollection<Document> collection, final Map<String, Object> query) {
        getCount(collection, query)
    }

    static long count(MongoCollection<Document> collection, final Map<String, Object> query, final ReadPreference readPreference) {
        getCount(collection, query, readPreference);
    }

    static long count(MongoCollection<Document> collection, final Map query, final Map<String, Object> options) {
        getCount(collection, query, options);
    }

    static long count(DBCollection collection, final Map query, final ReadPreference readPreference) {
        getCount(collection, query, null, readPreference);
    }

    static long getCount(DBCollection collection, final Map query) {
        getCount(collection, query, null);
    }

    static long getCount(MongoCollection<Document> collection, final Map<String, Object> query) {
        collection.count((Bson)new Document(query))
    }

    static long getCount(MongoCollection<Document> collection, final Map<String, Object> query, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .count((Bson)new Document(query))
    }

    static long getCount(MongoCollection<Document> collection, final Map<String, Object> query, final  Map<String, Object> options) {
        collection
                .count((Bson)new Document(query), mapToObject(CountOptions, options))
    }

    static long getCount(DBCollection collection, final Map query, final Map projection) {
        getCount(collection, query, projection, 0, 0);
    }

    static long getCount(DBCollection collection, final Map query, final Map projection, final ReadPreference readPreference) {
        getCount(collection, query, projection, 0, 0, readPreference)
    }


    static long getCount(DBCollection collection, final Map query, final Map projection, final long limit, final long skip) {
        collection.getCount((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), limit, skip)
    }


    static long getCount(DBCollection collection, final Map query, final Map projection, final long limit, final long skip,
                         final ReadPreference readPreference) {
        collection.getCount((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), limit, skip, readPreference)
    }

    static void createIndex(DBCollection collection, final Map keys, final String name) {
        createIndex(collection, keys, name, false)
    }

    static void createIndex(DBCollection collection, final Map keys, final String name, final boolean unique) {
        collection.createIndex((DBObject)new BasicDBObject(keys), name, unique)
    }

    static void createIndex(final DBCollection collection, final Map keys) {
        collection.createIndex((DBObject)new BasicDBObject(keys))
    }

    static void createIndex(final DBCollection collection, final Map keys, final Map options) {
        collection.createIndex((DBObject)new BasicDBObject(keys), (DBObject)new BasicDBObject(options))
    }

    static void createIndex(MongoCollection<Document> collection, final Map keys, final String name) {
        createIndex(collection, keys, name, false)
    }

    static void createIndex(MongoCollection<Document> collection, final Map keys, final String name, final boolean unique) {
        collection.createIndex((Bson)new Document(keys), new IndexOptions().name(name).unique(unique))
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys) {
        collection.createIndex((Bson)new Document(keys))
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys, final IndexOptions options) {
        collection.createIndex((Bson)new Document(keys), options)
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys, final Map<String, Object> options) {
        collection.createIndex((Bson)new Document(keys), mapToObject(IndexOptions, options))
    }
    static void dropIndex(final DBCollection collection, final Map index) {
        collection.dropIndex((DBObject)new BasicDBObject(index))
    }

    static void dropIndex(final MongoCollection<Document> collection, final Map<String, Object> index) {
        collection.dropIndex((Bson)new Document(index))
    }

    static WriteResult insert(final DBCollection collection, final Map document) {
        insert(collection, asList(document))
    }

    static void insert(final MongoCollection<Document> collection, final Map<String, Object> document) {
        insert(collection, asList(document))
    }

    static WriteResult leftShift(final DBCollection collection, final Map document) {
        insert(collection, document)
    }

    static WriteResult insert(final DBCollection collection, final Map document, final WriteConcern writeConcern) {
        insert(collection, asList(document), writeConcern);
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final Map<String, Object> document, final WriteConcern writeConcern) {
        insert(collection, asList(document), writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final Map... documents) {
        collection.insert documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static void insert(final MongoCollection<Document> collection, final Map<String, Object>... documents) {
        collection.insertMany documents.collect() { Map m -> new Document(m) } as List<Document>
    }

    static WriteResult leftShift(final DBCollection collection, final Map... documents) {
        insert(collection, documents)
    }

    static MongoCollection<Document> leftShift(final MongoCollection<Document> collection, final Map<String, Object>... documents) {
        insert(collection, documents)
        return collection
    }

    static WriteResult insert(final DBCollection collection, final WriteConcern writeConcern, final Map... documents) {
        insert(collection, documents, writeConcern);
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final WriteConcern writeConcern, final Map<String, Object>... documents) {
        insert(collection, documents, writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final Map[] documents, final WriteConcern writeConcern) {
        insert(collection, asList(documents), writeConcern);
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final Map<String,Object>[] documents, final WriteConcern writeConcern) {
        insert(collection, asList(documents), writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents) {
        collection.insert documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final List<? extends Map<String, Object>> documents) {
        collection.insertMany documents.collect() { Map m -> new Document(m) } as List<Document>
        return collection
    }

    static WriteResult leftShift(final DBCollection collection, final List<? extends Map> documents) {
        insert(collection, documents)
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents, final WriteConcern aWriteConcern) {
        return insert(collection, documents, aWriteConcern, null);
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final List<? extends Map<String, Object>> documents, final WriteConcern aWriteConcern) {
        return insert(collection, documents, aWriteConcern, null);
    }

    static WriteResult insert(final DBCollection collection, final Map[] documents, final WriteConcern aWriteConcern, final DBEncoder encoder) {
        return insert(collection, asList(documents), aWriteConcern, encoder);
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents, final WriteConcern aWriteConcern, final DBEncoder dbEncoder) {
        collection.insert( documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject> , aWriteConcern, dbEncoder)
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents, final InsertOptions insertOptions) {
        collection.insert documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject>, insertOptions
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final List<? extends Map<String, Object>> documents, final WriteConcern writeConcern, final InsertManyOptions insertOptions) {
        collection
                .withWriteConcern(writeConcern)
                .insertMany documents.collect() { Map m -> new Document(m) } as List<Document>, insertOptions
        return collection
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final List<? extends Map> documents, final InsertManyOptions insertOptions) {
        collection.insertMany documents.collect() { Map m -> new Document(m) } as List<Document>, insertOptions
        return collection
    }

    static  WriteResult save(final DBCollection collection, final Map document) {
        collection.save( (DBObject)new BasicDBObject(document) )
    }

    static WriteResult save(final DBCollection collection, final Map document, final WriteConcern writeConcern) {
        collection.save( (DBObject)new BasicDBObject(document), writeConcern )
    }

    static  MongoCollection save(final MongoCollection<Document> collection, final Map<String, Object> document) {
        insert collection, document
    }

    static  MongoCollection save(final MongoCollection<Document> collection, final Map<String, Object> document, final WriteConcern writeConcern) {
        insert collection, document, writeConcern
    }

    static UpdateResult updateOne(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateOne((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult update(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateOne((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult updateOne(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.updateOne((Bson)new Document(filter), new Document(update), mapToObject(UpdateOptions, options))
    }

    static UpdateResult update(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.updateOne((Bson)new Document(filter), new Document(update), mapToObject(UpdateOptions, options))
    }

    static WriteResult update(final DBCollection collection, final Map query, final Map update, final boolean upsert, final boolean multi,
                              final WriteConcern aWriteConcern) {
        collection.update((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(update), upsert, multi, aWriteConcern)
    }

    static WriteResult update(final DBCollection collection, final Map query, final Map update, final boolean upsert, final boolean multi, final WriteConcern aWriteConcern, final DBEncoder encoder) {
        collection.update((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(update), upsert, multi, aWriteConcern, encoder)
    }

    static WriteResult update(final DBCollection collection, final Map query, final Map update, final boolean upsert, final boolean multi) {
        collection.update((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(update), upsert, multi)
    }

    static WriteResult update(final DBCollection collection, final Map query, final Map update) {
        collection.update((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(update))
    }

    static UpdateResult updateMany(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateMany((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult updateMany(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.updateMany((Bson)new Document(filter), new Document(update), mapToObject(UpdateOptions, options))
    }

    static WriteResult updateMulti(final DBCollection collection, final Map query, final Map update) {
        collection.updateMulti((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(update))
    }

    static WriteResult remove(final DBCollection collection, final Map query) {
        collection.remove( (DBObject)new BasicDBObject(query) )
    }

    static WriteResult rightShift(final DBCollection collection, final Map query) {
        remove collection, query
    }

    static WriteResult remove(final DBCollection collection, final Map query, final WriteConcern writeConcern) {
        collection.remove( (DBObject)new BasicDBObject(query), writeConcern )
    }

    static WriteResult remove(final DBCollection collection, final Map query, final WriteConcern writeConcern, final DBEncoder encoder) {
        collection.remove( (DBObject)new BasicDBObject(query), writeConcern, encoder )
    }

    static DeleteResult deleteMany(final MongoCollection<Document> collection, final Map<String,Object> query) {
        collection.deleteMany( (Bson)new Document(query) )
    }

    static DeleteResult remove(final MongoCollection<Document> collection, final Map<String,Object> query) {
        deleteMany collection, query
    }

    static MongoCollection<Document> rightShift(final MongoCollection<Document> collection, final Map<String, Object> query) {
        deleteMany collection, query
        return collection
    }

    static DeleteResult deleteMany(final MongoCollection<Document> collection, final Map<String,Object> query, final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .deleteMany( (Bson) new Document(query) )
    }

    static DeleteResult deleteOne(final MongoCollection<Document> collection, final Map<String,Object> query) {
        collection.deleteOne( (Bson)new Document(query) )
    }

    static DeleteResult deleteOne(final MongoCollection<Document> collection, final Map<String,Object> query, final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .deleteOne( (Bson) new Document(query) )
    }

    static void setHintFields(final DBCollection collection, final List<? extends Map> indexes) {
        collection.hintFields = indexes.collect() {  Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static DBObject findAndModify(final DBCollection collection, final Map query, final Map sort, final Map update) {
        findAndModify(collection, query, null, sort, false, update, false, false);
    }

    static DBObject findAndModify(final DBCollection collection, final Map query, final Map update) {
        findAndModify(collection, query, null, null, false, update, false, false)
    }

    static DBObject findAndRemove(final DBCollection collection, final Map query) {
        findAndModify(collection, query, null, null, true, null, false, false);
    }

    static DBObject findAndModify(final DBCollection collection, final Map query, final Map fields, final Map sort,
                                  final boolean remove, final Map update,
                                  final boolean returnNew, final boolean upsert) {
        findAndModify(collection, query, fields, sort, remove, update, returnNew, upsert, 0L, MILLISECONDS)
    }

    static DBObject findAndModify(final DBCollection collection, final Map query, final Map fields, final Map sort,
                                  final boolean remove, final Map update,
                                  final boolean returnNew, final boolean upsert,
                                  final long maxTime, final TimeUnit maxTimeUnit) {
        collection.findAndModify((DBObject)new BasicDBObject(query), fields ? new BasicDBObject(fields) : null, sort ? new BasicDBObject(sort) : null, remove, update ? new BasicDBObject(update) : null, returnNew, upsert, maxTime, maxTimeUnit)
    }

    static List distinct(final DBCollection collection, final String fieldName, final Map query) {
        collection.distinct(fieldName, (DBObject)new BasicDBObject(query))
    }

    static List distinct(final DBCollection collection, final String fieldName, final Map query, final ReadPreference readPreference) {
        collection.distinct(fieldName, (DBObject)new BasicDBObject(query), readPreference)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName) {
        collection.distinct(fieldName, Document)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .distinct(fieldName, Document)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, Map<String, Object> query) {
        collection.distinct(fieldName, Document)
                  .filter( new Document(query) )
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, Map<String, Object> query, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .distinct(fieldName, Document)
                .filter( new Document(query) )
    }

    static AggregationOutput aggregate(final DBCollection collection, final List<? extends Map> pipeline) {
        collection.aggregate pipeline.collect() {  Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static AggregationOutput aggregate(final DBCollection collection, final List<? extends Map> pipeline, final ReadPreference readPreference) {
        collection.aggregate pipeline.collect() {  Map m -> new BasicDBObject(m) } as List<DBObject>, readPreference
    }

    static Cursor aggregate(final DBCollection collection, final List<? extends Map> pipeline, final AggregationOptions options) {
        collection.aggregate pipeline.collect() {  Map m -> new BasicDBObject(m) } as List<DBObject>, options
    }

    static DBObject group(final DBCollection collection, final Map key, final Map cond, final Map initial, final String reduce) {
        group(collection, key, cond, initial, reduce, null)
    }

    static DBObject group(final DBCollection collection, final Map key, final Map cond, final Map initial, final String reduce,
                          final String finalize) {
        collection.group((DBObject)new BasicDBObject(key), new BasicDBObject(cond), new BasicDBObject(initial), reduce, finalize)
    }

    static DBObject group(final DBCollection collection, final Map key, final Map cond, final Map initial, final String reduce,
                          final String finalize, final ReadPreference readPreference) {
        collection.group((DBObject)new BasicDBObject(key), new BasicDBObject(cond), new BasicDBObject(initial), reduce, finalize, readPreference)
    }

    static MapReduceOutput mapReduce(final DBCollection collection, final String map, final String reduce, final String outputTarget,
                                     final Map query) {
        collection.mapReduce(map, reduce, outputTarget, (DBObject) new BasicDBObject(query))
    }

    static MapReduceOutput mapReduce(final DBCollection collection, final String map, final String reduce, final String outputTarget,
                                     final MapReduceCommand.OutputType outputType, final Map query) {
        collection.mapReduce(map, reduce, outputTarget,outputType, (DBObject)new BasicDBObject(query))
    }

    static MapReduceOutput mapReduce(final DBCollection collection, final String map, final String reduce, final String outputTarget,
                                     final MapReduceCommand.OutputType outputType, final Map query,
                                     final ReadPreference readPreference) {
        collection.mapReduce(map, reduce,  outputTarget, outputType, (DBObject)new BasicDBObject(query), readPreference)
    }


    static UpdateResult replaceOne(MongoCollection<Document> collection, Map<String, Object> filter, Document replacement) {
        collection.replaceOne((Bson)new Document(filter), replacement)
    }

    static UpdateResult replaceOne(MongoCollection<Document> collection, Map<String, Object> filter, Document replacement, Map<String,Object> options) {
        collection.replaceOne((Bson)new Document(filter), replacement, mapToObject(UpdateOptions, options))
    }

    static Document findOneAndDelete(MongoCollection<Document> collection, Map<String, Object> filter) {
        collection.findOneAndDelete( (Bson)new Document(filter) )
    }

    static Document findOneAndDelete(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> options) {
        collection.findOneAndDelete( (Bson)new Document(filter), mapToObject(FindOneAndDeleteOptions, options) )
    }

    static Document findOneAndReplace(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> replacement) {
        collection.findOneAndReplace( (Bson)new Document(filter), new Document(replacement) )
    }

    static Document findOneAndReplace(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> replacement, Map<String, Object> options) {
        collection.findOneAndReplace( (Bson)new Document(filter), new Document(replacement), mapToObject(FindOneAndReplaceOptions, options) )
    }

    static Document findOneAndUpdate(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> update) {
        collection.findOneAndUpdate( (Bson)new Document(filter), new Document(update) )
    }

    static Document findOneAndUpdate(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.findOneAndUpdate( (Bson)new Document(filter), new Document(update), mapToObject(FindOneAndUpdateOptions, options) )
    }

}

