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
package org.grails.datastore.mapping.mongo.engine

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import groovy.transform.CompileStatic
import org.bson.Document
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.query.MongoDocumentQuery
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher


/**
 * An {@org.grails.datastore.mapping.engine.EntityPersister} that uses the MongoDB 3.0 {@link org.bson.codecs.configuration.CodecRegistry} infrastructure
 *
 * @author Graeme Rocher
 * @since 5.0.0
 */
@CompileStatic
class MongoCodecEntityPersister extends EntityPersister {

    protected MongoSession mongoSession

    MongoCodecEntityPersister(MappingContext mappingContext, PersistentEntity entity, MongoSession session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
        this.mongoSession = mongoSession;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        retrieveAllEntities pe, Arrays.asList(keys)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        MongoCollection mongoCollection = getMongoCollection(pe)

        Map<String,Object> query = [:]
        query.put(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, [(MongoQuery.MONGO_IN_OPERATOR): keys])
        MongoCursor<Document> cursor = mongoCollection
                .find(new Document(query), pe.getJavaClass())
                .iterator()
        new MongoDocumentQuery.MongoResultList(cursor,0, this)

    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        return null
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        MongoCollection mongoCollection = getMongoCollection(pe)
        Document idQuery = createIdQuery(key)
        mongoCollection
                    .find(idQuery, pe.javaClass)
                    .limit(1)
                    .first()
    }

    protected Document createIdQuery(Object key) {
        Map<String, Object> query = [:]
        query.put(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, key)
        def idQuery = new Document(query)
        idQuery
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        MongoCollection mongoCollection = getMongoCollection(pe)

        mongoCollection
            .insertOne(obj)
        return getObjectIdentifier(obj)
    }



    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        MongoCollection mongoCollection = getMongoCollection(pe)


        def id = getObjectIdentifier(obj)
        Document idQuery = createIdQuery(id)
        if(id) {

            mongoCollection.deleteOne(idQuery)
        }
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        MongoCollection mongoCollection = getMongoCollection(pe)
        Map<String, Object> keys = [:]
        keys.put(
                (AbstractMongoObectEntityPersister.MONGO_ID_FIELD),
                [(MongoQuery.MONGO_IN_OPERATOR): objects.collect() { getObjectIdentifier(it) }.findAll { it != null }]
        )

        def idQuery = new Document(keys)
        mongoCollection.deleteMany(idQuery)
    }

    @Override
    Query createQuery() {
        return null
    }

    @Override
    Serializable refresh(Object o) {
        throw new UnsupportedOperationException("Refresh not supported by codec entity persistence engine")
    }

    protected MongoCollection getMongoCollection(PersistentEntity pe) {
        def database = mongoSession.getDatabase(pe)
        def collection = mongoSession.getCollectionName(pe)

        MongoClient client = (MongoClient)mongoSession.nativeInterface

        MongoCollection mongoCollection = client
                .getDatabase(database)
                .getCollection(collection)
                .withDocumentClass(pe.javaClass)
        return mongoCollection
    }
}
