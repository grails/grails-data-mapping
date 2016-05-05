package org.grails.datastore.rx.mongodb.query

import com.mongodb.rx.client.FindObservable
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.internal.CodecRegistryEmbeddedQueryEncoder
import org.grails.datastore.rx.query.RxQuery
import rx.Observable

/**
 * Reactive query implementation for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxMongoQuery<T> extends MongoQuery implements RxQuery<T> {

    final RxMongoDatastoreClient datastoreClient

    RxMongoQuery(RxMongoDatastoreClient client, PersistentEntity entity) {
        super(null, entity)

        datastoreClient = client
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        throw new UnsupportedOperationException("Blocking queries not supported")
    }

    @Override
    Observable<T> findAll() {
        FindObservable findObservable = executeQuery()
        if(max > -1) {
            findObservable.limit(max)
        }
        if(offset > 0) {
            findObservable.skip(offset)
        }
        return findObservable.toObservable()
    }

    @Override
    Observable<T> singleResult() {
        FindObservable findObservable = executeQuery()
        findObservable.limit(1)
        return findObservable.first()
    }

    protected FindObservable executeQuery() {
        def queryObject = createQueryObject(entity)
        populateMongoQuery(new CodecRegistryEmbeddedQueryEncoder(datastoreClient.codecRegistry), queryObject, criteria, entity)

        def clazz = entity.javaClass
        def mongoCollection = datastoreClient.getCollection(entity, clazz)
        def findObservable = mongoCollection.find(queryObject, clazz)
        return findObservable
    }
}

