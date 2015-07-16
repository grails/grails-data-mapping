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
import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit

import static com.mongodb.AggregationOptions.OutputMode.INLINE
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
    static Object getAt(DB db, String name) {
        db.getCollection(name)
    }

    static DBCollection createCollection(DB db, final String collectionName, final Map options) {
        db.createCollection(collectionName, (DBObject)new BasicDBObject(options))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query) {
        collection.findOne((DBObject)new BasicDBObject(query))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBObject findOne(DBCollection collection, final Map query, final Map projection, final Map sort) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), (DBObject)new BasicDBObject(sort))
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
    static DBObject findOne(DBCollection collection, final Map query, final Map projection, final Map sort,
                            final ReadPreference readPreference) {
        collection.findOne((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection), (DBObject)new BasicDBObject(sort), readPreference)
    }


    /**
     * @see DBCollection#findOne(com.mongodb.DBObject)
     */
    static DBCursor find(DBCollection collection, final Map query) {
        collection.find((DBObject)new BasicDBObject(query))
    }

    /**
     * @see DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject)
     */
    static DBCursor find(DBCollection collection, final Map query, final Map projection) {
        collection.find((DBObject)new BasicDBObject(query), (DBObject)new BasicDBObject(projection))
    }


    static long count(DBCollection collection, final Map query) {
        getCount(collection, query, null)
    }

    static long count(DBCollection collection, final Map query, final ReadPreference readPreference) {
        getCount(collection, query, null, readPreference);
    }


    static long getCount(DBCollection collection, final Map query) {
        getCount(collection, query, null);
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

    static void dropIndex(final DBCollection collection, final Map index) {
        collection.dropIndex((DBObject)new BasicDBObject(index))
    }

    static WriteResult insert(final DBCollection collection, final Map document) {
        insert(collection, asList(document))
    }

    static WriteResult leftShift(final DBCollection collection, final Map document) {
        insert(collection, document)
    }

    static WriteResult insert(final DBCollection collection, final Map document, final WriteConcern writeConcern) {
        insert(collection, asList(document), writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final Map... documents) {
        collection.insert documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static WriteResult leftShift(final DBCollection collection, final Map... documents) {
        insert(collection, documents)
    }

    static WriteResult insert(final DBCollection collection, final WriteConcern writeConcern, final Map... documents) {
        insert(collection, documents, writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final Map[] documents, final WriteConcern writeConcern) {
        insert(collection, asList(documents), writeConcern);
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents) {
        collection.insert documents.collect() { Map m -> new BasicDBObject(m) } as List<DBObject>
    }

    static WriteResult leftShift(final DBCollection collection, final List<? extends Map> documents) {
        insert(collection, documents)
    }

    static WriteResult insert(final DBCollection collection, final List<? extends Map> documents, final WriteConcern aWriteConcern) {
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

    static  WriteResult save(final DBCollection collection, final Map document) {
        collection.save( (DBObject)new BasicDBObject(document) )
    }

    static WriteResult save(final DBCollection collection, final Map document, final WriteConcern writeConcern) {
        collection.save( (DBObject)new BasicDBObject(document), writeConcern )
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

}

