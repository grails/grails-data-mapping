package org.grails.datastore.rx.mongodb.query

import com.mongodb.rx.client.AggregateObservable
import com.mongodb.rx.client.FindObservable
import groovy.transform.CompileStatic
import org.bson.Document
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.internal.CodecRegistryEmbeddedQueryEncoder
import org.grails.datastore.rx.query.RxQuery
import rx.Observable
import rx.Subscriber

/**
 * Reactive query implementation for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxMongoQuery extends MongoQuery implements RxQuery {

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
    Observable findAll() {

        final List<Query.Projection> projectionList = projections().getProjectionList()
        if(projectionList.isEmpty()) {
            FindObservable findObservable = executeQuery()
            if(max > -1) {
                findObservable.limit(max)
            }
            if(offset > 0) {
                findObservable.skip(offset)
            }
            return findObservable.toObservable()
        }
        else {
            return executeQuery(projectionList)
        }
    }

    @Override
    Observable singleResult() {
        final List<Query.Projection> projectionList = projections().getProjectionList()
        if(projectionList.isEmpty()) {
            FindObservable findObservable = executeQuery()
            findObservable.limit(1)
            return findObservable.toObservable()
        }
        else {
            return executeQuery(projectionList).first()
        }
    }

    protected Observable executeQuery(List<Query.Projection> projectionList) {
        def query = createQueryObject(entity)
        def aggregatePipeline = buildAggregatePipeline(entity, query, projectionList)
        List<Document> aggregationPipeline = aggregatePipeline.getAggregationPipeline()
        boolean singleResult = aggregatePipeline.isSingleResult()
        List<MongoQuery.ProjectedProperty> projectedKeys = aggregatePipeline.getProjectedKeys()


        def mongoCollection = datastoreClient.getCollection(entity, entity.javaClass)
        def aggregateObservable = mongoCollection.aggregate(aggregationPipeline)
        def observable = aggregateObservable.toObservable()
        if(singleResult) {
            return observable.map { Document d ->
                List projectedResults = []
                for(def property in projectedKeys) {
                    Object value = d.get(property.projectionKey);
                    if(value != null) {
                        projectedResults.add(value)
                    }
                    else {
                        if (property.projection instanceof Query.CountProjection) {
                            projectedResults.add(0)
                        }
                    }
                }
                if(projectedResults.size() == 1) {
                    return projectedResults[0]
                }
                else {
                    return projectedResults
                }
            }.switchIfEmpty(Observable.create({ Subscriber s ->
                for(def property in projectedKeys) {
                    if (property.projection instanceof Query.CountProjection) {
                        s.onNext(0)
                    }
                }

                s.onCompleted()
            } as Observable.OnSubscribe))
        }
        else {
            return observable
        }
    }

    protected FindObservable executeQuery() {
        def query = createQueryObject(entity)
        populateMongoQuery(new CodecRegistryEmbeddedQueryEncoder(datastoreClient.codecRegistry), query, criteria, entity)

        def clazz = entity.javaClass
        def mongoCollection = datastoreClient.getCollection(entity, clazz)

        def findObservable = mongoCollection.find(query, clazz)
        return findObservable
    }
}

